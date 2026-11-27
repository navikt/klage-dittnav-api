package no.nav.klage.service

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.createTestKlanke
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.exception.AttachmentGoneException
import no.nav.klage.domain.exception.AttachmentMissingFilenameException
import no.nav.klage.domain.exception.AttachmentNotFoundException
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import org.springframework.web.multipart.MultipartFile
import java.util.Optional

class VedleggServiceTest {
    private val klankeRepository = mockk<KlankeRepository>()
    private val image2PDF = mockk<Image2PDF>(relaxed = true)
    private val attachmentValidator = mockk<AttachmentValidator>(relaxed = true)
    private val vedleggMetrics = mockk<VedleggMetrics>(relaxed = true)
    private val fileClient = mockk<FileClient>(relaxed = true)
    private val validationService = mockk<ValidationService>(relaxed = true)

    private val vedleggService =
        VedleggService(
            klankeRepository = klankeRepository,
            image2PDF = image2PDF,
            attachmentValidator = attachmentValidator,
            vedleggMetrics = vedleggMetrics,
            fileClient = fileClient,
            validationService = validationService,
        )

    @Test
    fun `addKlankevedlegg with null filename throws typed exception without reading bytes or side effects`() {
        val klanke = createTestKlanke(status = KlageAnkeStatus.DRAFT)
        val multipart = mockk<MultipartFile>()
        every { klankeRepository.findById(klanke.id) } returns Optional.of(klanke)
        every { multipart.originalFilename } returns null

        val exception =
            assertThrows<AttachmentMissingFilenameException> {
                vedleggService.addKlankevedlegg(klankeId = klanke.id, multipart = multipart)
            }

        assertThat(exception.message).isEqualTo("MISSING_FILENAME")
        verify(exactly = 1) { validationService.checkKlankeStatus(klanke) }
        verify(exactly = 1) { validationService.validateKlankeAccess(klanke) }
        verify(exactly = 0) { multipart.bytes }
        verify { vedleggMetrics wasNot Called }
        verify { attachmentValidator wasNot Called }
        verify { image2PDF wasNot Called }
        verify { fileClient wasNot Called }
        assertThat(klanke.vedlegg).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(value = KlageAnkeStatus::class, names = ["SENDING", "DONE"])
    fun `getVedleggFromKlanke on finalized klanke throws AttachmentGoneException after access check without calling file store`(
        status: KlageAnkeStatus,
    ) {
        val klanke = createTestKlanke(status = status)
        val vedlegg = Vedlegg(tittel = "vedlegg.pdf", ref = "ref", contentType = "application/pdf", sizeInBytes = 1)
        klanke.vedlegg.add(vedlegg)
        every { klankeRepository.findById(klanke.id) } returns Optional.of(klanke)

        assertThrows<AttachmentGoneException> {
            vedleggService.getVedleggFromKlanke(klankeId = klanke.id, vedleggId = vedlegg.id)
        }

        verify(exactly = 1) { validationService.validateKlankeAccess(klanke) }
        verify { fileClient wasNot Called }
    }

    @Test
    fun `getVedleggFromKlanke on draft fetches file from file store`() {
        val klanke = createTestKlanke(status = KlageAnkeStatus.DRAFT)
        val vedlegg = Vedlegg(tittel = "vedlegg.pdf", ref = "ref", contentType = "application/pdf", sizeInBytes = 1)
        klanke.vedlegg.add(vedlegg)
        val bytes = byteArrayOf(1, 2, 3)
        every { klankeRepository.findById(klanke.id) } returns Optional.of(klanke)
        every { fileClient.getVedleggFile("ref") } returns bytes

        val result = vedleggService.getVedleggFromKlanke(klankeId = klanke.id, vedleggId = vedlegg.id)

        assertThat(result.content).isEqualTo(bytes)
        assertThat(result.filename).isEqualTo("vedlegg.pdf")
    }

    @ParameterizedTest
    @CsvSource(
        "bilde.jpg, bilde.pdf",
        "min.klage.png, min.klage.pdf",
        "uten_endelse, uten_endelse.pdf",
        ".png, vedlegg.pdf",
    )
    fun `getVedleggFromKlanke returns filename based on tittel with pdf extension`(
        tittel: String,
        expectedFilename: String,
    ) {
        val klanke = createTestKlanke(status = KlageAnkeStatus.DRAFT)
        val vedlegg = Vedlegg(tittel = tittel, ref = "ref", contentType = "image/png", sizeInBytes = 1)
        klanke.vedlegg.add(vedlegg)
        every { klankeRepository.findById(klanke.id) } returns Optional.of(klanke)
        every { fileClient.getVedleggFile("ref") } returns byteArrayOf(1)

        val result = vedleggService.getVedleggFromKlanke(klankeId = klanke.id, vedleggId = vedlegg.id)

        assertThat(result.filename).isEqualTo(expectedFilename)
    }

    @Test
    fun `getVedleggFromKlanke on draft with missing file logs error and rethrows AttachmentNotFoundException`() {
        val klanke = createTestKlanke(status = KlageAnkeStatus.DRAFT)
        val vedlegg = Vedlegg(tittel = "vedlegg.pdf", ref = "ref", contentType = "application/pdf", sizeInBytes = 1)
        klanke.vedlegg.add(vedlegg)
        every { klankeRepository.findById(klanke.id) } returns Optional.of(klanke)
        every { fileClient.getVedleggFile("ref") } throws AttachmentNotFoundException()

        val logger = LoggerFactory.getLogger(VedleggService::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(appender)
        try {
            assertThrows<AttachmentNotFoundException> {
                vedleggService.getVedleggFromKlanke(klankeId = klanke.id, vedleggId = vedlegg.id)
            }
        } finally {
            logger.detachAppender(appender)
        }

        val errors = appender.list.filter { it.level == Level.ERROR }
        assertThat(errors).hasSize(1)
        assertThat(errors.single().formattedMessage)
            .contains(vedlegg.id.toString())
            .contains(klanke.id.toString())
    }
}
