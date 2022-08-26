package no.nav.klage.domain.ankevedleggOLD

data class AnkeVedleggOLD(
    val tittel: String,
    val ref: String,
    val ankeInternalSaksnummer: String,
    val contentType: String = "Ukjent",
    val id: Int? = null,
    val sizeInBytes: Int
)

fun AnkeVedleggOLD.toAnkeVedleggView(content: String) =
    AnkeVedleggOLDView(tittel, ref, ankeInternalSaksnummer, contentType, id!!, sizeInBytes, content)