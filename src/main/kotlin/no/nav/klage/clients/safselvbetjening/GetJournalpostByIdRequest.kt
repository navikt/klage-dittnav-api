package no.nav.klage.clients.safselvbetjening

import no.nav.klage.clients.pdl.GraphqlQuery

data class GetJournalpostByIdGraphqlQuery(
    val query: String,
    val variables: GetJournalpostByIdVariables
)

data class GetJournalpostByIdVariables(
    val journalpostId: String
)

fun getJournalpostByIdQuery(journalpostId: String): GetJournalpostByIdGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/safselvbetjening/getJournalpostById.graphql").readText()
        .replace("[\n\r]", "")
    return GetJournalpostByIdGraphqlQuery(
        query = query,
        variables = GetJournalpostByIdVariables(journalpostId = journalpostId)
    )
}
