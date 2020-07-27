package no.nav.klage.domain.vedlegg

data class Vedlegg(
    val tittel: String,
    val ref: String,
    val klageId: Int,
    val contentType: String = "Ukjent",
    val id: Int? = null,
    val sizeInBytes: Int
)

fun Vedlegg.toVedleggView(content: String) = VedleggView(tittel, ref, klageId, contentType, id!!, sizeInBytes, content)