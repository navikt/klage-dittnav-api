package no.nav.klage.clients.safselvbetjening

data class GetDokumentoversiktResponse(val data: DokumentoversiktSelvbetjeningResponse?, val errors: List<PdlError>?)
data class DokumentoversiktSelvbetjeningResponse( val dokumentoversiktSelvbetjening: DokumentoversiktSelvbetjening)
data class DokumentoversiktSelvbetjening(val tema: List<TemaFromSafselvbetjening>)

data class TemaFromSafselvbetjening(
    val kode: String?,
    val navn: String?,
)