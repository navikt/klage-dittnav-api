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
