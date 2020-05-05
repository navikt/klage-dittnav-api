package no.nav.klage.services.saf

import no.nav.klage.domain.Vedtak
import java.time.LocalDateTime

data class SafResponse(
    val data: SafData
)

data class SafData(
    val dokumentoversiktBruker: DokumentoversiktBruker
)

data class DokumentoversiktBruker(
    val journalposter: List<Journalpost>
)

data class Journalpost(
    val journalpostId: String,
    val tittel: String,
    val tema: String,
    val journalfoerendeEnhet: String,
    val datoOpprettet: LocalDateTime
)

fun mapSafResponseToVedtak(response: SafResponse): List<Vedtak> =
    response.data.dokumentoversiktBruker.journalposter.map {
        Vedtak(
            title = it.tittel,
            tema = it.tema,
            journalfoerendeEnhet = it.journalfoerendeEnhet,
            vedtaksdato = it.datoOpprettet.toLocalDate()
        )
    }
