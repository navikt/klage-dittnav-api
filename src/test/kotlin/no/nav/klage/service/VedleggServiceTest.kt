package no.nav.klage.service

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.clients.FileClient
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.createTestKlanke
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.exception.AttachmentMissingFilenameException
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.vedlegg.AttachmentValidator
import no.nav.klage.vedlegg.Image2PDF
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

        assertEquals("MISSING_FILENAME", exception.message)
        verify(exactly = 1) { validationService.checkKlankeStatus(klanke) }
        verify(exactly = 1) { validationService.validateKlankeAccess(klanke) }
        verify(exactly = 0) { multipart.bytes }
        verify { vedleggMetrics wasNot Called }
        verify { attachmentValidator wasNot Called }
        verify { image2PDF wasNot Called }
        verify { fileClient wasNot Called }
        assertTrue(klanke.vedlegg.isEmpty())
    }
}
