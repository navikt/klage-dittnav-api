package no.nav.klage.domain.ankevedlegg

data class AnkeVedleggView(
    val tittel: String,
    val ref: String,
    val ankeId: Int,
    val contentType: String = "Ukjent",
    val id: Int,
    val sizeInBytes: Int,
    val content: String
)

fun AnkeVedleggView.toAnkeVedlegg() = AnkeVedlegg(
    tittel = tittel,
    ref = ref,
    ankeId = ankeId,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes
)