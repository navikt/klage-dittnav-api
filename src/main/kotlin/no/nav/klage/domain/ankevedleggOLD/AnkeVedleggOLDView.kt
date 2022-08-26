package no.nav.klage.domain.ankevedleggOLD

data class AnkeVedleggOLDView(
    val tittel: String,
    val ref: String,
    val ankeInternalSaksnummer: String,
    val contentType: String = "Ukjent",
    val id: Int,
    val sizeInBytes: Int,
    val content: String
)

fun AnkeVedleggOLDView.toAnkeVedlegg() = AnkeVedleggOLD(
    tittel = tittel,
    ref = ref,
    ankeInternalSaksnummer = ankeInternalSaksnummer,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes
)