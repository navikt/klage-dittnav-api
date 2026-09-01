package no.nav.klage.clients.safselvbetjening

data class GetJournalpostByIdResponse(
    val data: GetJournalpostById?,
    val errors: List<GraphQLError>?,
)

data class GetJournalpostById(
    val journalpostById: JournalpostById?,
)

data class JournalpostById(
    val journalpostId: String,
    val tittel: String,
    val dokumenter: List<Dokument>,
)

data class Dokument(
    val dokumentInfoId: String,
)

data class GraphQLError(
    val message: String,
    val locations: List<ErrorLocation>,
    val path: List<String>?,
    val extensions: ErrorExtension,
)

data class ErrorLocation(
    val line: Int?,
    val column: Int?,
)

data class ErrorExtension(
    val code: String?,
    val classification: String,
)
