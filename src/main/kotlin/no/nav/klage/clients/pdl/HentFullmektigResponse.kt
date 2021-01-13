package no.nav.klage.clients.pdl

data class HentFullmektigResponse(val data: HentFullmektig?, val errors: List<PdlError>?)

data class HentFullmektig(val hentPerson: FullmektigWrapper?)

data class FullmektigWrapper(val fullmakt: List<Fullmakt>)