package no.nav.klage.domain

import java.time.LocalDate

data class AggregatedKlage(
    val id: Int,
    val klageInstans: Boolean,
    val trygderetten: Boolean,
    val navn: String,
    val adresse: String,
    val telefon: String,
    val navenhet: String,
    val vedtaksdato: LocalDate,
    val navReferanse: String,
    val kortRedegjoerelse: String,
    val sted: String,
    val dato: LocalDate,
    val oversiktVedlegg: String,
    val begrunnelse: String,
    val foedselsnummer: String,
    val tema: String,
    val vedlegg: List<Vedlegg>
)

fun createAggregatedKlage(
    klage: Klage
): AggregatedKlage =
    AggregatedKlage(
        id = klage.id!!,
        klageInstans = false, // TODO: False for MVP
        trygderetten = false, // TODO: False for MVP
        navn = "Get from bruker",
        adresse = "Get from bruker",
        telefon = "Get from bruker",
        navenhet = klage.enhetId ?: "Ukjent enhet", // TODO: How to handle?
        vedtaksdato = LocalDate.now(), // TODO: Get from frontend
        navReferanse = "Get from front end?",
        kortRedegjoerelse = klage.fritekst,
        sted = "Get from front end",
        dato = LocalDate.from(klage.modifiedByUser),
        oversiktVedlegg = "???",
        begrunnelse = klage.fritekst,
        foedselsnummer = "From token or bruker",
        tema = klage.tema.name,
        vedlegg = klage.vedlegg ?: listOf()
    )
