package no.nav.klage.vedlegg

import io.mockk.every
import io.mockk.mockk
import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.domain.exception.AttachmentEncryptedException
import no.nav.klage.domain.exception.AttachmentHasVirusException
import no.nav.klage.domain.exception.AttachmentIsEmptyException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

internal class AttachmentValidatorTest {

    lateinit var validator: AttachmentValidator
    lateinit var clamAvClient: ClamAvClient

    @BeforeEach
    fun beforeEach() {
        clamAvClient = mockk()
        validator = AttachmentValidator(clamAvClient)
    }

    @Test
    fun `empty file throws AttachmentIsEmptyException`() {
        val fileMock = mockk<File>()
        every { fileMock.length() } returns 0L
        assertThrows<AttachmentIsEmptyException> {
            validator.validateAttachment(fileMock)
        }
    }

    @Test
    fun `file with virus throws AttachmentHasVirusException`() {
        val fileMock = mockk<File>()
        every { fileMock.length() } returns 1L
        every { clamAvClient.hasVirus(any()) } returns true
        assertThrows<AttachmentHasVirusException> {
            validator.validateAttachment(fileMock)
        }
    }

    @Test
    fun `pdf with password throws AttachmentEncryptedException`() {
        val file = File("src/test/resources/pdf/pdf-with-user-password.pdf")

        every { clamAvClient.hasVirus(any()) } returns false
        assertThrows<AttachmentEncryptedException> {
            validator.validateAttachment(file)
        }
    }

    @Test
    fun `pdf with empty password works`() {
        val file = File("src/test/resources/pdf/pdf-with-empty-user-password.pdf")
        every { clamAvClient.hasVirus(any()) } returns false
        validator.validateAttachment(file)
    }

    @Test
    fun `valid file passes validation`() {
        val file = File("src/test/resources/pdf/test123.pdf")
        every { clamAvClient.hasVirus(any()) } returns false
        validator.validateAttachment(file)
    }

}
