package no.nav.klage.domain.ankevedlegg

data class AnkeVedleggView(
    val tittel: String,
    val ref: String,
    val ankeInternalSaksnummer: String,
    val contentType: String = "Ukjent",
    val id: Int,
    val sizeInBytes: Int,
    val content: String
)

fun AnkeVedleggView.toAnkeVedlegg() = AnkeVedlegg(
    tittel = tittel,
    ref = ref,
    ankeInternalSaksnummer = ankeInternalSaksnummer,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes
)