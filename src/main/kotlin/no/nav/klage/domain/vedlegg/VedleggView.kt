package no.nav.klage.domain.vedlegg

data class VedleggView(
    val tittel: String,
    val ref: String,
    val klageId: Int,
    val contentType: String = "Ukjent",
    val id: Int,
    val sizeInBytes: Int,
    val content: String
)

fun VedleggView.toVedlegg() = Vedlegg(
    tittel = tittel,
    ref = ref,
    klageId = klageId,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes
)