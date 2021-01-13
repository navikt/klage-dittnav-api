package no.nav.klage.domain.klage

import no.nav.klage.domain.vedlegg.Vedlegg
import java.time.LocalDate

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
    val dato: LocalDate,
    val begrunnelse: String,
    val identifikasjonstype: String,
    val identifikasjonsnummer: String,
    val tema: String,
    val ytelse: String,
    val vedlegg: List<Vedlegg>,
    val userChoices: List<String>?,
    val userSaksnummer: String? = null,
    val internalSaksnummer: String?,
    val fullmektigNavn: String?,
    val fullmektigFnr: String?
)

