package no.nav.klage.domain.ankevedlegg

data class AnkeVedlegg(
    val tittel: String,
    val ref: String,
    val ankeInternalSaksnummer: String,
    val contentType: String = "Ukjent",
    val id: Int? = null,
    val sizeInBytes: Int
)

fun AnkeVedlegg.toAnkeVedleggView(content: String) =
    AnkeVedleggView(tittel, ref, ankeInternalSaksnummer, contentType, id!!, sizeInBytes, content)