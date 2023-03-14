package no.nav.klage.domain.anke

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.util.getInnsendingsytelse
import java.time.LocalDate

data class AnkeInput(
    val tema: Tema?,
    val titleKey: Innsendingsytelse?,
    val internalSaksnummer: String?,
    val innsendingsytelse: Innsendingsytelse?,
) {
    fun toAnke(bruker: Bruker): Anke {
        val innsendingsytelse = getInnsendingsytelse(titleKey = titleKey, innsendingsytelse = innsendingsytelse)
        return Anke(
            foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = innsendingsytelse.getTema(),
            language = LanguageEnum.NB,
            innsendingsytelse = innsendingsytelse,
            internalSaksnummer = internalSaksnummer,
        )
    }
}

data class AnkeFullInput(
    val tema: Tema?,
    val titleKey: Innsendingsytelse?,
    val innsendingsytelse: Innsendingsytelse?,
    val userSaksnummer: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fritekst: String?,
    val enhetsnummer: String? = null,
    val hasVedlegg: Boolean,
) {
    fun toAnke(bruker: Bruker): Anke {
        val innsendingsytelse = getInnsendingsytelse(titleKey = titleKey, innsendingsytelse = innsendingsytelse)
        return Anke(
            foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = innsendingsytelse.getTema(),
            innsendingsytelse = innsendingsytelse,
            userSaksnummer = userSaksnummer,
            language = language,
            vedtakDate = vedtakDate,
            internalSaksnummer = internalSaksnummer,
            fritekst = fritekst,
            enhetsnummer = enhetsnummer,
            hasVedlegg = hasVedlegg,
        )
    }
}


