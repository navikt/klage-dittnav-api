package no.nav.klage.config

import no.nav.klage.clients.clamav.ClamAvClient
import no.nav.klage.vedlegg.AttachmentValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize

@Configuration
class AttachmentValidatorConfig(private val clamAvClient: ClamAvClient) {

    @Value("\${maxAttachmentSize}")
    private lateinit var maxAttachmentSizeAsString: String

    @Value("\${maxTotalSize}")
    private lateinit var maxTotalSizeAsString: String

    @Bean
    fun attachmentValidator(): AttachmentValidator {
        return AttachmentValidator(
            clamAvClient,
            DataSize.parse(maxAttachmentSizeAsString),
            DataSize.parse(maxTotalSizeAsString)
        )
    }

}