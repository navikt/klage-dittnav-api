package no.nav.klage.config.problem

import no.nav.klage.domain.exception.AttachmentMissingFilenameException
import no.nav.klage.domain.exception.AttachmentTooLargeException
import no.nav.klage.domain.exception.InvalidProperty
import no.nav.klage.domain.exception.SectionedValidationErrorWithDetailsException
import no.nav.klage.domain.exception.ValidationSection
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

class GlobalExceptionHandlerTest {
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
    }
}
