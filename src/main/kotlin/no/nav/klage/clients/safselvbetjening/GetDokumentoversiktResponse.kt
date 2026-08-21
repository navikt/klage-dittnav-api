package no.nav.klage.clients.safselvbetjening

data class GetDokumentoversiktResponse(val data: DokumentoversiktSelvbetjeningResponse?, val errors: List<GraphQLError>?)
data class DokumentoversiktSelvbetjeningResponse( val dokumentoversiktSelvbetjening: DokumentoversiktSelvbetjening)
data class DokumentoversiktSelvbetjening(val tema: List<TemaFromSafSelvbetjening>)

data class TemaFromSafSelvbetjening(
    val kode: String?,
    val navn: String?,
)