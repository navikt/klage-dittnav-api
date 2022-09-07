package no.nav.klage.domain.vedlegg

data class VedleggView(
    val tittel: String,
    val contentType: String = "Ukjent",
    val id: Int,
    val sizeInBytes: Int,
)