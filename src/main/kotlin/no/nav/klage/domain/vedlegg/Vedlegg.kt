package no.nav.klage.domain.vedlegg

data class Vedlegg(
    val tittel: String,
    val ref: String,
    val klageId: Int,
    val contentType: String = "Ukjent",
    val id: Int? = null,
    val sizeInBytes: Int
)

fun Vedlegg.toVedleggView() = VedleggView(
    tittel = tittel,
    contentType = contentType,
    id = id!!,
    sizeInBytes = sizeInBytes,
)