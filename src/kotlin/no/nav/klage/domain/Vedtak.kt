package no.nav.klage.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class Vedtak(
    val title: String,
    val vedtaksdato: LocalDate,
    val tema: String,
    val journalfoerendeEnhet: String
)

fun mapSafResponseToVedtak(response: SafResponse): List<Vedtak> =
    response.data.journalposter.map {
        Vedtak(
            title = it.tittel,
            tema = it.tema,
            journalfoerendeEnhet = it.journalfoerendeEnhet,
            vedtaksdato = it.datoOpprettet.toLocalDate()
        )
    }

data class SafResponse(
    val data: DokumentoversiktBruker
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

data class SafRequest(
    val query: String,
    val variables: SafVariables
)

data class BrukerIdInput(
    val id: String,
    val type: String = "FNR"
)

data class SafVariables(
    val brukerId: BrukerIdInput,
    val journalposttyper: List<String> = listOf("U")
)
