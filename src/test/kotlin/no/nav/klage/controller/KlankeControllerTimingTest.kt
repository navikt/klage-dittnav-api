package no.nav.klage.controller

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.events.KafkaEventClient
import no.nav.klage.config.SseShutdownSignal
import no.nav.klage.config.TIMED_REQUEST_CONTROLLER_ENTRY_NANOS
import no.nav.klage.controller.view.StringInput
import no.nav.klage.domain.jpa.Vedlegg
import no.nav.klage.service.CommonService
import no.nav.klage.service.VedleggService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDateTime
import java.util.UUID

class KlankeControllerTimingTest {
    @Test
    fun `marks controller entry after multipart argument resolution`() {
        val vedleggService = mockk<VedleggService>()
        val klankeId = UUID.randomUUID()
        val vedlegg = MockMultipartFile("vedlegg", "test.pdf", "application/pdf", byteArrayOf(1))
        val request = MockHttpServletRequest()
        every { vedleggService.addKlankevedlegg(klankeId = klankeId, multipart = vedlegg) } returns
            Vedlegg(tittel = "test.pdf", ref = "ref", contentType = "application/pdf", sizeInBytes = 1)
        val controller =
            KlankeController(
                vedleggService = vedleggService,
                kafkaEventClient = mockk<KafkaEventClient>(),
                commonService = mockk<CommonService>(),
                sseShutdownSignal = SseShutdownSignal(),
            )

        controller.addVedleggToKlanke(klankeId, vedlegg, request)

        assertTrue(request.getAttribute(TIMED_REQUEST_CONTROLLER_ENTRY_NANOS) is Long)
    }

    @Test
    fun `marks controller entry after fritekst argument resolution`() {
        val commonService = mockk<CommonService>()
        val klankeId = UUID.randomUUID()
        val request = MockHttpServletRequest()
        every { commonService.updateFritekst(klankeId = klankeId, fritekst = "private text") } returns LocalDateTime.now()
        val controller =
            KlankeController(
                vedleggService = mockk<VedleggService>(),
                kafkaEventClient = mockk<KafkaEventClient>(),
                commonService = commonService,
                sseShutdownSignal = SseShutdownSignal(),
            )

        controller.updateFritekst(klankeId = klankeId, input = StringInput("private text"), request = request)

        assertTrue(request.getAttribute(TIMED_REQUEST_CONTROLLER_ENTRY_NANOS) is Long)
    }
}
