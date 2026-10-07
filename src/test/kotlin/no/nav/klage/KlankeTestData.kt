package no.nav.klage

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Type
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

fun createTestKlanke(
    status: KlageAnkeStatus,
    markedCompleted: LocalDateTime? = null,
    modifiedByUser: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
    id: UUID = UUID.randomUUID(),
): Klanke =
    Klanke(
        id = id,
        foedselsnummer = "12345678910",
        fritekst = "fritekst",
        status = status,
        userSaksnummer = null,
        journalpostId = null,
        vedtakDate = null,
        sak = null,
        language = LanguageEnum.NB,
        innsendingsytelse = Innsendingsytelse.ARBEIDSAVKLARINGSPENGER,
        hasVedlegg = false,
        pdfDownloaded = null,
        vedlegg = mutableSetOf(),
        created = modifiedByUser,
        modifiedByUser = modifiedByUser,
        type = Type.KLAGE,
        caseIsAtKA = null,
        fullmektigFoedselsnummer = null,
        markedCompleted = markedCompleted,
    )

fun createTestAggregatedKlageAnke(id: UUID = UUID.randomUUID()): AggregatedKlageAnke =
    AggregatedKlageAnke(
        id = id.toString(),
        identifikasjonsnummer = "12345678910",
        fornavn = "Test",
        mellomnavn = "",
        etternavn = "Testesen",
        vedtak = "Ikke angitt",
        dato = LocalDate.now(),
        begrunnelse = "fritekst",
        ytelse = "Arbeidsavklaringspenger",
        vedlegg = emptyList(),
        userSaksnummer = null,
        internalSaksnummer = null,
        sak = null,
        klageAnkeType = AggregatedKlageAnke.KlageAnkeType.KLAGE,
        fullmektigId = null,
        innsendingsYtelseId = Innsendingsytelse.ARBEIDSAVKLARINGSPENGER.id,
        ettersendelseTilKa = null,
    )
