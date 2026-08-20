package no.nav.klage.clients.safselvbetjening

data class GetDokumentoversiktResponse(val data: Dokumentoversikt?, val errors: List<PdlError>?)

data class Dokumentoversikt(val tema: List<TemaFromSafselvbetjening>?)

data class TemaFromSafselvbetjening(
    val kode: String?,
    val navn: String?,
)