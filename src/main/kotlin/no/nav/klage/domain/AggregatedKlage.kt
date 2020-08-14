package no.nav.klage.domain

import no.nav.klage.domain.klage.Klage
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import no.nav.klage.domain.vedlegg.Vedlegg

data class AggregatedKlage(
    val id: Int,
    val klageInstans: Boolean,
    val trygderetten: Boolean,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val adresse: String,
    val telefon: String,
    val vedtak: String,
    val saksnummer: String?,
    val dato: LocalDate,
    val begrunnelse: String,
    val identifikasjonstype: String,
    val identifikasjonsnummer: String,
    val tema: String,
    val ytelse: String,
    val vedlegg: List<Vedlegg>
)

fun createAggregatedKlage(
    bruker: Bruker,
    klage: Klage
): AggregatedKlage {

    return AggregatedKlage(
        id = klage.id!!,
        klageInstans = false, // TODO: False for MVP
        trygderetten = false, // TODO: False for MVP
        fornavn = bruker.navn.fornavn,
        mellomnavn = bruker.navn.mellomnavn ?: "",
        etternavn = bruker.navn.etternavn,
        adresse = bruker.adresse?.toKlageskjemaString() ?: "Ukjent adresse",
        telefon = bruker.kontaktinformasjon?.telefonnummer ?: "",
        vedtak = klage.vedtak,
        saksnummer = klage.saksnummer,
        dato = ZonedDateTime.ofInstant(klage.modifiedByUser, UTC).toLocalDate(),
        begrunnelse = klage.fritekst,
        identifikasjonstype = bruker.folkeregisteridentifikator.type,
        identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
        tema = klage.tema.name,
        ytelse = klage.ytelse,
        vedlegg = klage.vedlegg
    )
}