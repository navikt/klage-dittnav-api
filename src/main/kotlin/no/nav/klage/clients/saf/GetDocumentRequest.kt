package no.nav.klage.clients.saf
data class GetDocumentRequest(
    val journalpostId: String,
    val dokumentInfoId: String,
    val variantFormat: String = "ARKIV",
)
