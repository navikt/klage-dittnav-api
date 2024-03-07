package no.nav.klage.domain.klage

import java.time.LocalDate

data class AggregatedKlageAnke(
    val id: String,
    val identifikasjonsnummer: String,
    val fornavn: String,
    val mellomnavn: String,
    val etternavn: String,
    val vedtak: String,
    val dato: LocalDate,
    val begrunnelse: String,
    val tema: String,
    val ytelse: String,
    val vedlegg: List<Vedlegg>,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val klageAnkeType: KlageAnkeType,
    //klage specific
    val userChoices: List<String>?,
    //anke specific
    val enhetsnummer: String?,
) {
    enum class KlageAnkeType {
        KLAGE, ANKE, KLAGE_ETTERSENDELSE, ANKE_ETTERSENDELSE,
    }

    data class Vedlegg(
        val tittel: String,
        val ref: String,
    )
}


