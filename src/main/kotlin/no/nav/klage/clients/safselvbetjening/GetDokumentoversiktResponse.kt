package no.nav.klage.clients.safselvbetjening

data class GetDokumentoversiktResponse(val data: TemaList?, val errors: List<PdlError>?)

data class TemaList(val tema: List<TemaFromSafselvbetjening>)

data class TemaFromSafselvbetjening(
    val kode: String,
    val navn: String,
)