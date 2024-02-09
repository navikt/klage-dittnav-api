package no.nav.klage.controller.view

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.jpa.Anke
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.jpa.Vedlegg

fun Anke.toAnkeView(): AnkeView {
    return AnkeView(
        id = id.toString(),
        fritekst = fritekst ?: "",
        status = status,
        modifiedByUser = modifiedByUser,
        vedtakDate = vedtakDate,
        userSaksnummer = userSaksnummer,
        language = language,
        innsendingsytelse = innsendingsytelse,
        hasVedlegg = hasVedlegg,
        enhetsnummer = enhetsnummer,
        vedlegg = vedlegg.map { it.toVedleggView() },
        journalpostId = journalpostId,
        finalizedDate = if (status === KlageAnkeStatus.DONE) modifiedByUser.toLocalDate() else null,
        internalSaksnummer = internalSaksnummer,
    )
}

fun Klage.toKlageView(): KlageView {
    return KlageView(
        id = id,
        fritekst = fritekst ?: "",
        status = status,
        modifiedByUser = modifiedByUser,
        vedtakDate = vedtakDate,
        userSaksnummer = userSaksnummer,
        language = language,
        innsendingsytelse = innsendingsytelse,
        hasVedlegg = hasVedlegg,
        vedlegg = vedlegg.map { it.toVedleggView() },
        journalpostId = journalpostId,
        finalizedDate = if (status === KlageAnkeStatus.DONE) modifiedByUser.toLocalDate() else null,
        internalSaksnummer = internalSaksnummer,
        checkboxesSelected = checkboxesSelected.toSet(),
    )
}

fun Vedlegg.toVedleggView() = VedleggView(
    tittel = tittel,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes,
)