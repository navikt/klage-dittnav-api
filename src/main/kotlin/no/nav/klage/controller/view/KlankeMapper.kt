package no.nav.klage.controller.view

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.isFinalized
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Vedlegg

fun Klanke.toKlankeView(userHasDocumentForThisTema: Boolean?): KlankeView =
    KlankeView(
        id = id,
        fritekst = fritekst ?: "",
        status = if (status == KlageAnkeStatus.SENDING) KlageAnkeStatus.DONE else status,
        modifiedByUser = modifiedByUser,
        vedtakDate = vedtakDate,
        userSaksnummer = userSaksnummer,
        language = language,
        innsendingsytelse = innsendingsytelse,
        hasVedlegg = hasVedlegg,
        vedlegg = vedlegg.map { it.toVedleggView() },
        journalpostId = journalpostId,
        finalizedDate = if (status.isFinalized()) (markedCompleted?.toLocalDate() ?: modifiedByUser.toLocalDate()) else null,
        internalSaksnummer = sak?.fagsakid,
        sakFagsaksystem = sak?.fagsaksystem,
        sakSakstype = sak?.sakstype,
        type = type,
        caseIsAtKA = caseIsAtKA,
        userHasDocumentForThisTema = userHasDocumentForThisTema,
    )

fun Klanke.toKlankeFinalizedView(): KlankeFinalizedView =
    KlankeFinalizedView(
        modifiedByUser = modifiedByUser,
        finalizedDate =
            markedCompleted?.toLocalDate()
                // Legacy DONE rows were finalized before marked_completed existed.
                ?: if (status === KlageAnkeStatus.DONE) {
                    modifiedByUser.toLocalDate()
                } else {
                    error("Klanke $id with status $status has no markedCompleted")
                },
    )

fun Vedlegg.toVedleggView() =
    VedleggView(
        tittel = tittel,
        contentType = contentType,
        id = id,
        sizeInBytes = sizeInBytes,
    )
