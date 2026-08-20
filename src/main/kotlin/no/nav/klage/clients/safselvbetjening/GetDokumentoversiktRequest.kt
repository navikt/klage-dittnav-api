package no.nav.klage.clients.safselvbetjening

import no.nav.klage.clients.pdl.GraphqlQuery

data class GetDokumentoversiktGraphqlQuery(
    val query: String,
    val variables: GetDokumentoversiktVariables
)

data class GetDokumentoversiktVariables(
    val ident: String
)

fun getDokumentoversiktQuery(ident: String): GetDokumentoversiktGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/safselvbetjening/getDokumentoversikt.graphql").readText()
        .replace("[\n\r]", "")
    return GetDokumentoversiktGraphqlQuery(
        query = query,
        variables = GetDokumentoversiktVariables(ident = ident)
    )
}