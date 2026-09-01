package no.nav.klage.clients.safselvbetjening

data class GetJournalpostByIdGraphqlQuery(
    val query: String,
    val variables: GetJournalpostByIdVariables,
)

data class GetJournalpostByIdVariables(
    val journalpostId: String,
)

fun getJournalpostByIdQuery(journalpostId: String): GetJournalpostByIdGraphqlQuery {
    val query =
        GetJournalpostByIdGraphqlQuery::class.java
            .getResource("/safselvbetjening/getJournalpostById.graphql")
            .readText()
            .replace(oldValue = "[\n\r]", newValue = "")
    return GetJournalpostByIdGraphqlQuery(
        query = query,
        variables = GetJournalpostByIdVariables(journalpostId = journalpostId),
    )
}
