package no.nav.klage.controller

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.config.SseShutdownSignal
import no.nav.klage.config.problem.GlobalExceptionHandler
import no.nav.klage.createTestKlanke
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Type
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.service.CommonService
import no.nav.klage.service.RepresentasjonService
import no.nav.klage.service.ValidationService
import no.nav.klage.util.TokenUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.Optional

class KlankeValidationTest {
    private val klanke = createTestKlanke(status = KlageAnkeStatus.DRAFT)
    private val originalModifiedByUser = klanke.modifiedByUser
    private val originalCreated = klanke.created
    private val klankeRepository = mockk<KlankeRepository>()
    private val tokenUtil = mockk<TokenUtil>()
    private val representasjonService = mockk<RepresentasjonService>()
    private val applicationEventPublisher = mockk<ApplicationEventPublisher>()
    private val validationService = ValidationService(representasjonService = representasjonService, tokenUtil = tokenUtil)
    private val commonService =
        CommonService(
            klankeRepository = klankeRepository,
            validationService = validationService,
            klageAnkeMetrics = mockk(),
            vedleggMetrics = mockk(),
            klageDittnavPdfgenService = mockk(),
            documentService = mockk(),
            representasjonService = representasjonService,
            klageLookupClient = mockk(),
            tokenUtil = tokenUtil,
            safSelvbetjeningService = mockk(),
            applicationEventPublisher = applicationEventPublisher,
            brukerService = mockk(),
        )
    private val mockMvc =
        MockMvcBuilders
            .standaloneSetup(
                KlankeController(
                    vedleggService = mockk(),
                    kafkaEventClient = mockk(),
                    commonService = commonService,
                    sseShutdownSignal = SseShutdownSignal(),
                ),
            ).setControllerAdvice(GlobalExceptionHandler())
            .build()
    private val validationPath = "/api/klanker/${klanke.id}/validate"

    @BeforeEach
    fun setup() {
        every { klankeRepository.findById(klanke.id) } returns Optional.of(klanke)
        every { tokenUtil.getSubject() } returns klanke.foedselsnummer
    }

    @AfterEach
    fun `validation does not write or publish events`() {
        verify(exactly = 1) { klankeRepository.findById(klanke.id) }
        verify(exactly = 0) { klankeRepository.save(any()) }
        verify { applicationEventPublisher wasNot Called }
        assertEquals(originalModifiedByUser, klanke.modifiedByUser)
        assertEquals(originalCreated, klanke.created)
        assertNull(klanke.markedCompleted)
        assertNull(klanke.journalpostId)
        assertNull(klanke.pdfDownloaded)
        assertTrue(klanke.vedlegg.isEmpty())
    }

    @Test
    fun `valid stored draft returns no content without finalizing`() {
        mockMvc
            .perform(post(validationPath))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        assertEquals(KlageAnkeStatus.DRAFT, klanke.status)
        assertEquals("fritekst", klanke.fritekst)
    }

    @Test
    fun `invalid stored draft returns validation sections without finalizing`() {
        klanke.fritekst = null

        mockMvc
            .perform(post(validationPath))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Validation error"))
            .andExpect(jsonPath("$.sections[0].section").value("klankedata"))
            .andExpect(jsonPath("$.sections[0].properties[0].field").value("fritekst,vedlegg"))
            .andExpect(jsonPath("$.sections[0].properties[0].reason").isNotEmpty)

        assertEquals(KlageAnkeStatus.DRAFT, klanke.status)
        assertNull(klanke.fritekst)
    }

    @Test
    fun `ettersendelse returns all missing fields`() {
        klanke.fritekst = null
        klanke.type = Type.KLAGE_ETTERSENDELSE

        mockMvc
            .perform(post(validationPath))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.sections[0].properties.length()").value(2))
            .andExpect(jsonPath("$.sections[0].properties[1].field").value("caseIsAtKa"))
    }

    @Test
    fun `unknown klanke returns not found`() {
        every { klankeRepository.findById(klanke.id) } returns Optional.empty()

        mockMvc.perform(post(validationPath)).andExpect(status().isNotFound)

        verify { tokenUtil wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(value = KlageAnkeStatus::class, names = ["DRAFT", "DELETED", "SENDING", "DONE"])
    fun `non-owner cannot see status or validation errors`(klankeStatus: KlageAnkeStatus) {
        klanke.status = klankeStatus
        klanke.fritekst = null
        every { tokenUtil.getSubject() } returns "other-user"

        mockMvc
            .perform(post(validationPath))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.sections").doesNotExist())

        assertEquals(klankeStatus, klanke.status)
    }

    @ParameterizedTest
    @EnumSource(value = KlageAnkeStatus::class, names = ["DELETED", "SENDING", "DONE"])
    fun `deleted and finalized klanker are rejected before content validation`(klankeStatus: KlageAnkeStatus) {
        klanke.status = klankeStatus
        klanke.fritekst = null

        mockMvc
            .perform(post(validationPath))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.sections").doesNotExist())

        assertEquals(klankeStatus, klanke.status)
    }

    @Test
    fun `authorized representative can validate draft`() {
        klanke.fullmektigFoedselsnummer = "representative"
        every { tokenUtil.getSubject() } returns "representative"
        every { representasjonService.representasjonIsValid(fullmaktsgiverId = klanke.foedselsnummer, tema = any()) } returns true

        mockMvc.perform(post(validationPath)).andExpect(status().isNoContent)

        assertEquals(KlageAnkeStatus.DRAFT, klanke.status)
    }

    @Test
    fun `revoked fullmakt is rejected before status or content validation`() {
        klanke.fullmektigFoedselsnummer = "representative"
        klanke.status = KlageAnkeStatus.DONE
        klanke.fritekst = null
        every { tokenUtil.getSubject() } returns "representative"
        every { representasjonService.representasjonIsValid(fullmaktsgiverId = klanke.foedselsnummer, tema = any()) } returns false

        mockMvc
            .perform(post(validationPath))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.sections").doesNotExist())
    }

    @Test
    fun `another representative cannot validate klanke`() {
        klanke.fullmektigFoedselsnummer = "representative"
        every { tokenUtil.getSubject() } returns "other-representative"

        mockMvc.perform(post(validationPath)).andExpect(status().isNotFound)

        verify { representasjonService wasNot Called }
    }
}
