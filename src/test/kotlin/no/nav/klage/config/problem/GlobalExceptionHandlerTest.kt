package no.nav.klage.config.problem

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import no.nav.klage.domain.exception.AttachmentEncryptedException
import no.nav.klage.domain.exception.AttachmentGoneException
import no.nav.klage.domain.exception.AttachmentMissingFilenameException
import no.nav.klage.domain.exception.AttachmentNotFoundException
import no.nav.klage.domain.exception.AttachmentTooLargeException
import no.nav.klage.domain.exception.InvalidProperty
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.domain.exception.SectionedValidationErrorWithDetailsException
import no.nav.klage.domain.exception.ValidationSection
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.AsyncRequestTimeoutException

class GlobalExceptionHandlerTest {
    companion object {
        private val CLIENT_ERROR = AttributeKey.stringKey("request.client_error")
    }

    @Test
    fun `sectioned validation errors return bad request with all validation details`() {
        val exception =
            SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections =
                    listOf(
                        ValidationSection(
                            section = "klankedata",
                            properties =
                                listOf(
                                    InvalidProperty(field = "fritekst,vedlegg", reason = "Text or attachment required"),
                                    InvalidProperty(field = "caseIsAtKa", reason = "Required"),
                                ),
                        ),
                        ValidationSection(
                            section = "other",
                            properties = listOf(InvalidProperty(field = "field", reason = "Invalid")),
                        ),
                    ),
            )

        mockMvc(exception)
            .perform(post("/validation"))
            .andExpect(status().isBadRequest)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value(exception.title))
            .andExpect(jsonPath("$.detail").value(exception.title))
            .andExpect(jsonPath("$.sections.length()").value(2))
            .andExpect(jsonPath("$.sections[0].section").value("klankedata"))
            .andExpect(jsonPath("$.sections[0].properties.length()").value(2))
            .andExpect(jsonPath("$.sections[0].properties[0].field").value("fritekst,vedlegg"))
            .andExpect(jsonPath("$.sections[0].properties[0].reason").value("Text or attachment required"))
            .andExpect(jsonPath("$.sections[0].properties[1].field").value("caseIsAtKa"))
            .andExpect(jsonPath("$.sections[0].properties[1].reason").value("Required"))
            .andExpect(jsonPath("$.sections[1].section").value("other"))
            .andExpect(jsonPath("$.sections[1].properties[0].field").value("field"))
            .andExpect(jsonPath("$.sections[1].properties[0].reason").value("Invalid"))
    }

    @Test
    fun `sectioned validation errors with no sections still return bad request`() {
        val exception = SectionedValidationErrorWithDetailsException(title = "Validation error", sections = emptyList())

        mockMvc(exception)
            .perform(post("/validation"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.title").value(exception.title))
            .andExpect(jsonPath("$.sections").isEmpty)
    }

    @Test
    fun `attachment too large returns payload too large`() {
        mockMvc(AttachmentTooLargeException())
            .perform(post("/validation"))
            .andExpect(status().isPayloadTooLarge)
            .andExpect(jsonPath("$.status").value(413))
            .andExpect(jsonPath("$.title").value("TOO_LARGE"))
    }

    @Test
    fun `attachment missing filename returns bad request`() {
        mockMvc(AttachmentMissingFilenameException())
            .perform(post("/validation"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("MISSING_FILENAME"))
    }

    @Test
    fun `attachment gone returns gone`() {
        mockMvc(AttachmentGoneException())
            .perform(post("/validation"))
            .andExpect(status().isGone)
            .andExpect(jsonPath("$.status").value(410))
            .andExpect(jsonPath("$.title").value("ATTACHMENT_GONE"))
    }

    @Test
    fun `attachment not found returns not found`() {
        mockMvc(AttachmentNotFoundException())
            .perform(post("/validation"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("ATTACHMENT_NOT_FOUND"))
    }

    @Test
    fun `bad request sets client error title on current span`() {
        val span = mockCurrentSpan()
        try {
            mockMvc(AttachmentEncryptedException())
                .perform(post("/validation"))
                .andExpect(status().isBadRequest)

            verify { span.setAttribute(CLIENT_ERROR, "ENCRYPTED") }
            verify(exactly = 0) { span.recordException(any()) }
        } finally {
            unmockkStatic(Span::class)
        }
    }

    @Test
    fun `sectioned validation error sets its title on current span`() {
        val span = mockCurrentSpan()
        try {
            mockMvc(SectionedValidationErrorWithDetailsException(title = "Validation error", sections = emptyList()))
                .perform(post("/validation"))
                .andExpect(status().isBadRequest)

            verify { span.setAttribute(CLIENT_ERROR, "Validation error") }
        } finally {
            unmockkStatic(Span::class)
        }
    }

    @Test
    fun `spring built-in bad request sets exception type as client error on current span`() {
        val span = mockCurrentSpan()
        try {
            mockMvc(IllegalStateException("unused"))
                .perform(post("/param"))
                .andExpect(status().isBadRequest)

            verify { span.setAttribute(CLIENT_ERROR, "MissingServletRequestParameterException") }
        } finally {
            unmockkStatic(Span::class)
        }
    }

    @Test
    fun `other client error sets title on current span`() {
        val span = mockCurrentSpan()
        try {
            mockMvc(AttachmentTooLargeException())
                .perform(post("/validation"))
                .andExpect(status().isPayloadTooLarge)

            verify { span.setAttribute(CLIENT_ERROR, "TOO_LARGE") }
        } finally {
            unmockkStatic(Span::class)
        }
    }

    @Test
    fun `not found sets title on current span`() {
        val span = mockCurrentSpan()
        try {
            mockMvc(KlankeNotFoundException())
                .perform(post("/validation"))
                .andExpect(status().isNotFound)

            verify { span.setAttribute(CLIENT_ERROR, "Klage/anke not found") }
        } finally {
            unmockkStatic(Span::class)
        }
    }

    @Test
    fun `spring built-in client error other than bad request sets exception type on current span`() {
        val span = mockCurrentSpan()
        try {
            mockMvc(IllegalStateException("unused"))
                .perform(get("/param"))
                .andExpect(status().isMethodNotAllowed)

            verify { span.setAttribute(CLIENT_ERROR, "HttpRequestMethodNotSupportedException") }
        } finally {
            unmockkStatic(Span::class)
        }
    }

    @Test
    fun `server error does not set client error on current span`() {
        val span = mockCurrentSpan()
        try {
            mockMvc(AsyncRequestTimeoutException())
                .perform(post("/validation"))
                .andExpect(status().isServiceUnavailable)

            verify(exactly = 0) { span.setAttribute(CLIENT_ERROR, any<String>()) }
        } finally {
            unmockkStatic(Span::class)
        }
    }

    private fun mockCurrentSpan(): Span {
        val span = mockk<Span>(relaxed = true)
        mockkStatic(Span::class)
        every { Span.current() } returns span
        return span
    }

    private fun mockMvc(exception: Exception) =
        MockMvcBuilders
            .standaloneSetup(ValidationController(exception))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()

    @RestController
    class ValidationController(
        private val exception: Exception,
    ) {
        @PostMapping("/validation")
        fun validate(): Unit = throw exception

        @PostMapping("/param")
        fun param(
            @RequestParam required: String,
        ): String = required
    }
}
