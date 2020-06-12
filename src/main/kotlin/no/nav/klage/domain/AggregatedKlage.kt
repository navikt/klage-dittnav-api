package no.nav.klage.domain

import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

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
    val dato: LocalDate,
    val oversiktVedlegg: String,
    val begrunnelse: String,
    val foedselsnummer: String,
    val tema: String,
    val vedlegg: List<Vedlegg>
)

fun createAggregatedKlage(
    bruker: Bruker,
    klage: Klage
): AggregatedKlage =
    AggregatedKlage(
        id = klage.id!!,
        klageInstans = false, // TODO: False for MVP
        trygderetten = false, // TODO: False for MVP
        navn = bruker.navn.toKlageskjemaString(),
        adresse = bruker.adresse?.toKlageskjemaString() ?: "Ukjent adresse",
        telefon = bruker.kontaktinformasjon?.telefonnummer ?: "",
        navenhet = klage.enhetId ?: "Ukjent enhet",
        vedtaksdato = klage.vedtaksdato,
        navReferanse = klage.referanse ?: "Ikke angitt",
        kortRedegjoerelse = klage.fritekst,
        dato = ZonedDateTime.ofInstant(klage.modifiedByUser, UTC).toLocalDate(),
        oversiktVedlegg = "???",
        begrunnelse = klage.fritekst,
        foedselsnummer = "From token or bruker",
        tema = klage.tema.name,
        vedlegg = klage.vedlegg
    )
