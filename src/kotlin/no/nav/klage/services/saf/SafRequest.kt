package no.nav.klage.services.saf

data class SafRequest(
    val query: String,
    val variables: SafVariables
)

data class BrukerIdInput(
    val id: String,
    val type: String = "FNR"
)

data class SafVariables(
    val brukerId: BrukerIdInput,
    val journalposttyper: List<String> = listOf("U")
)

fun journalposterQuery(fnr: String): SafRequest {
    val query = SafRequest::class.java.getResource("/saf/journalposter.graphql").readText().replace("[\n\r]", "")
    return SafRequest(query, SafVariables(BrukerIdInput(fnr)))
}
