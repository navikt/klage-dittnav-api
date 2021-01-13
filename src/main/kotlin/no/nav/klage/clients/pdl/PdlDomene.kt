package no.nav.klage.clients.pdl

enum class IdentGruppe {
    FOLKEREGISTERIDENT, NPID, AKTORID
}

data class GraphqlQuery(
    val query: String,
    val variables: Variables
)

data class PersonGraphqlQuery(
    val query: String,
    val variables: HentPersonVariabler
)

data class HentPersonVariabler(
    val ident: String
)

data class FullmektigGraphqlQuery(
    val query: String,
    val variables: HentFullmektigVariabler
)

data class HentFullmektigVariabler(
    val ident: String
)

data class Variables(
    val ident: String,
    val navnHistorikk: Boolean,
    val grupper: List<IdentGruppe> = listOf(IdentGruppe.AKTORID, IdentGruppe.FOLKEREGISTERIDENT, IdentGruppe.NPID)
)

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension
)

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String
)

fun hentPersonQuery(fnr: String): PersonGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/pdl/hentPerson.graphql").readText().replace("[\n\r]", "")
    return PersonGraphqlQuery(query, HentPersonVariabler(fnr))
}

fun hentFullmektigQuery(fnr: String): FullmektigGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/pdl/hentFullmektig.graphql").readText().replace("[\n\r]", "")
    return FullmektigGraphqlQuery(query, HentFullmektigVariabler(fnr))
}