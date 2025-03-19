package no.nav.klage.controller.view

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Vedlegg

fun Klanke.toKlankeView(): KlankeView {
    return KlankeView(
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
        internalSaksnummer = sak?.fagsakid,
        sakFagsaksystem = sak?.fagsaksystem,
        sakSakstype = sak?.sakstype,
        checkboxesSelected = checkboxesSelected.toSet(),
        type = type,
        caseIsAtKA = caseIsAtKA,
    )
}

fun Vedlegg.toVedleggView() = VedleggView(
    tittel = tittel,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes,
)