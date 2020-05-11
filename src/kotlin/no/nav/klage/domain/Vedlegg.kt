package no.nav.klage.domain

import org.springframework.web.multipart.MultipartFile
import java.util.*

data class Vedlegg(
    val content: MultipartFile,
    val title: String,
    val id: UUID
) {
    fun contentAsBytes(): ByteArray = content.bytes
}
