package no.nav.klage.service

import no.nav.klage.clients.klagelookup.KlageLookupClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.KlankeFinalizedView
import no.nav.klage.controller.view.KlankeInput
import no.nav.klage.controller.view.KlankeView
import no.nav.klage.controller.view.OpenKlankeInput
import no.nav.klage.controller.view.toKlankeFinalizedView
import no.nav.klage.controller.view.toKlankeView
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.KlankeMarkedCompletedEvent
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Navn
import no.nav.klage.domain.Type
import no.nav.klage.domain.exception.KlankeNotFoundException
import no.nav.klage.domain.journalpostIdEvent
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Sak
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToTema
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
@Transactional
class CommonService(
    private val klankeRepository: KlankeRepository,
    private val validationService: ValidationService,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService,
    private val documentService: DocumentService,
    private val representasjonService: RepresentasjonService,
    private val klageLookupClient: KlageLookupClient,
    private val tokenUtil: TokenUtil,
    private val safSelvbetjeningService: SafSelvbetjeningService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val brukerService: BrukerService,
) {
    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    private fun createKlanke(
        input: KlankeInput,
        foedselsnummer: String,
        fullmektigFoedselsnummer: String?,
    ): Klanke {
        val klanke = input.toKlanke(foedselsnummer = foedselsnummer, fullmektigFoedselsnummer = fullmektigFoedselsnummer)
        return klankeRepository.save(klanke).also {
            updateMetrics(input = klanke)
        }
    }

    private fun KlankeInput.toKlanke(
        foedselsnummer: String,
        fullmektigFoedselsnummer: String?,
    ): Klanke =
        Klanke(
            foedselsnummer = foedselsnummer,
            fritekst = null,
            status = KlageAnkeStatus.DRAFT,
            userSaksnummer = null,
            journalpostId = null,
            vedtakDate = null,
            sak =
                Sak(
                    fagsakid = internalSaksnummer,
                    sakstype = sakSakstype,
                    fagsaksystem = sakFagsaksystem,
                ),
            language = LanguageEnum.NB,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = false,
            created = LocalDateTime.now(),
            modifiedByUser = LocalDateTime.now(),
            pdfDownloaded = null,
            type = type,
            caseIsAtKA = caseIsAtKA,
            fullmektigFoedselsnummer = fullmektigFoedselsnummer,
        )

    private fun updateMetrics(input: Klanke) {
        val temaReport =
            if (klageAnkeIsLonnskompensasjon(innsendingsytelse = input.innsendingsytelse)) {
                LOENNSKOMPENSASJON_GRAFANA_TEMA
            } else {
                innsendingsytelseToTema[input.innsendingsytelse]!!.name
            }
        klageAnkeMetrics.incrementKlankerInitialized(
            ytelse = temaReport,
            type = input.type,
        )
    }

    fun getDraftOrCreateKlanke(input: KlankeInput): KlankeView {
        val loggedInUser = tokenUtil.getSubject()
        val currentSakenGjelder = input.sakenGjelder ?: loggedInUser
        val currentFullmektig = if (currentSakenGjelder != loggedInUser) loggedInUser else null
        if (currentFullmektig != null) {
            validationService.validateFullmakt(
                fullmaktsgiverId = currentSakenGjelder,
                tema = innsendingsytelseToTema[input.innsendingsytelse]!!,
            )
        }
        val existingKlanke =
            getLatestKlankeDraft(
                foedselsnummer = currentSakenGjelder,
                internalSaksnummer = input.internalSaksnummer,
                innsendingsytelse = input.innsendingsytelse,
                type = input.type,
                fullmektigFoedselsnummer = currentFullmektig,
            )

        if (existingKlanke != null && input.caseIsAtKA != null) {
            existingKlanke.caseIsAtKA = input.caseIsAtKA
        }

        return existingKlanke?.toKlankeView(
            userHasDocumentForThisTema =
                userHasDocumentForThisTema(
                    innsendingsytelse = existingKlanke.innsendingsytelse,
                    userIdent = existingKlanke.foedselsnummer,
                    documentCheckAction = DocumentCheckAction.OTHER,
                    type = existingKlanke.type,
                ),
            fullmaktsgivere = getFullmaktsgivere(existingKlanke.innsendingsytelse),
        )
            ?: createKlanke(
                input = input,
                foedselsnummer = currentSakenGjelder,
                fullmektigFoedselsnummer = currentFullmektig,
            ).toKlankeView(
                userHasDocumentForThisTema =
                    userHasDocumentForThisTema(
                        innsendingsytelse = input.innsendingsytelse,
                        userIdent = currentSakenGjelder,
                        documentCheckAction = DocumentCheckAction.CREATE,
                        type = input.type,
                    ),
                fullmaktsgivere = getFullmaktsgivere(input.innsendingsytelse),
            )
    }

    private fun getFullmaktsgivere(innsendingsytelse: Innsendingsytelse): List<Bruker> =
        innsendingsytelseToTema[innsendingsytelse]?.let {
            representasjonService
                .getFullmaktsgivereForTema(
                    it,
                ).map {
                    brukerService.getBruker(fnr = it, tema = innsendingsytelseToTema[innsendingsytelse])
                }
        } ?: emptyList()

    fun getLatestKlankeDraft(
        foedselsnummer: String,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse,
        type: Type,
        fullmektigFoedselsnummer: String?,
    ): Klanke? =
        klankeRepository
            .findByFoedselsnummerAndStatusAndTypeAndFullmektigFoedselsnummer(
                fnr = foedselsnummer,
                status = KlageAnkeStatus.DRAFT,
                type = type,
                fullmektigFoedselsnummer = fullmektigFoedselsnummer,
            ).filter {
                if (internalSaksnummer != null) {
                    it.innsendingsytelse == innsendingsytelse && it.sak?.fagsakid == internalSaksnummer
                } else {
                    it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser }

    /**
     * Marks the klanke as completed by the user. Does not send to Kafka; KlankeSenderService does that after commit.
     */
    fun finalizeKlanke(klankeId: UUID): KlankeFinalizedView {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.validateKlankeAccess(klanke = existingKlanke)
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)
        if (existingKlanke.isFinalized()) {
            logger.debug("Klanke is already finalized, returning.")
            return existingKlanke.toKlankeFinalizedView()
        }
        validationService.validateKlanke(klanke = existingKlanke)
        val now = LocalDateTime.now()
        existingKlanke.status = KlageAnkeStatus.SENDING
        existingKlanke.modifiedByUser = now
        existingKlanke.markedCompleted = now

        applicationEventPublisher.publishEvent(KlankeMarkedCompletedEvent(klankeId = existingKlanke.id))
        registerFinalizedDocumentCheck(klanke = existingKlanke)
        return existingKlanke.toKlankeFinalizedView()
    }

    fun getKlankeWithoutValidation(klankeId: UUID): Klanke = klankeRepository.findById(klankeId).get()

    fun findKlankeIdsToSend(): List<UUID> = klankeRepository.findByStatus(KlageAnkeStatus.SENDING).map { it.id }

    /**
     * Sets status DONE for a klanke confirmed sent to Kafka. markedCompleted and modifiedByUser are left unchanged.
     */
    fun markKlankeAsDone(klankeId: UUID) {
        val klanke = klankeRepository.findById(klankeId).get()
        if (klanke.status != KlageAnkeStatus.SENDING) {
            logger.warn("Klanke {} has status {}, expected SENDING. Not marking as DONE.", klankeId, klanke.status)
            return
        }
        klanke.status = KlageAnkeStatus.DONE
        registerFinalizedMetrics(klanke = klanke)

        logger.debug(
            "Klanke {} med innsendingsytelse {} er sendt inn.",
            klankeId,
            klanke.innsendingsytelse.name,
        )
    }

    // Requires user token (SAF selvbetjening OBO). Must run in the user's request.
    private fun registerFinalizedDocumentCheck(klanke: Klanke) {
        userHasDocumentForThisTema(
            innsendingsytelse = klanke.innsendingsytelse,
            userIdent = klanke.foedselsnummer,
            documentCheckAction = DocumentCheckAction.FINALIZE,
            type = klanke.type,
        )
    }

    private fun registerFinalizedMetrics(klanke: Klanke) {
        val temaReport =
            if (klageAnkeIsLonnskompensasjon(klanke.innsendingsytelse)) {
                LOENNSKOMPENSASJON_GRAFANA_TEMA
            } else {
                innsendingsytelseToTema[klanke.innsendingsytelse]!!.name
            }

        if (klanke.type == Type.KLAGE) {
            klageAnkeMetrics.incrementKlagerFinalizedTitle(klanke.innsendingsytelse)
        }

        klageAnkeMetrics.incrementKlankerFinalized(ytelse = temaReport, type = klanke.type)

        if (klanke.userSaksnummer != null) {
            klageAnkeMetrics.incrementOptionalSaksnummer(temaReport)
        }
        if (klanke.vedtakDate != null) {
            klageAnkeMetrics.incrementOptionalVedtaksdato(temaReport)
        }

        vedleggMetrics.registerNumberOfVedleggPerUser(klanke.vedlegg.size.toDouble())
    }

    fun getKlankePdf(klankeId: UUID): Pair<Path, String> {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        val loggedInUser = tokenUtil.getSubject()
        if (loggedInUser != existingKlanke.foedselsnummer) {
            throw KlankeNotFoundException()
        }
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlanke)
        requireNotNull(existingKlanke.journalpostId)

        return documentService.getPathToDocumentPdfAndTitle(existingKlanke.journalpostId!!)
    }

    fun createKlankePdfWithFoersteside(klankeId: UUID): ByteArray? {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)
        validationService.validateKlankeAccess(
            klanke = existingKlanke,
        )

        validationService.validateKlanke(klanke = existingKlanke)

        klageDittnavPdfgenService
            .createKlankePdfWithFoersteside(
                createPdfWithFoerstesideInput(klanke = existingKlanke),
            ).also {
                setPdfDownloadedWithoutAccessValidation(
                    klankeId = klankeId,
                    pdfDownloaded = LocalDateTime.now(),
                )
                return it
            }
    }

    // Uses system user token, so it works outside a user request (async listener and scheduler).
    fun createAggregatedKlankeAsSystemUser(klanke: Klanke): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(klanke.vedtakDate) ?: "Ikke angitt"
        val userInKlanke = klageLookupClient.getPersonAsSystemUser(fnr = klanke.foedselsnummer)

        return AggregatedKlageAnke(
            id = klanke.id.toString(),
            fornavn = userInKlanke.fornavn,
            mellomnavn = userInKlanke.mellomnavn ?: "",
            etternavn = userInKlanke.etternavn,
            vedtak = vedtak,
            dato = klanke.markedCompleted?.toLocalDate() ?: klanke.modifiedByUser.toLocalDate(),
            begrunnelse = sanitizeText(klanke.fritekst ?: ""),
            identifikasjonsnummer = klanke.foedselsnummer,
            ytelse = klanke.innsendingsytelse.nbName,
            vedlegg = klanke.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
            userSaksnummer = klanke.userSaksnummer,
            internalSaksnummer = klanke.sak?.fagsakid,
            klageAnkeType = AggregatedKlageAnke.KlageAnkeType.valueOf(klanke.type.name),
            ettersendelseTilKa = klanke.caseIsAtKA,
            innsendingsYtelseId = klanke.innsendingsytelse.id,
            sak =
                if (klanke.sak?.fagsakid != null && klanke.sak?.sakstype != null && klanke.sak?.fagsaksystem != null) {
                    AggregatedKlageAnke.Sak(
                        sakstype = klanke.sak?.sakstype!!,
                        fagsaksystem = klanke.sak?.fagsaksystem!!,
                        fagsakid = klanke.sak?.fagsakid!!,
                    )
                } else {
                    null
                },
            fullmektigId = klanke.fullmektigFoedselsnummer,
        )
    }

    fun createPdfWithFoerstesideInput(klanke: Klanke): OpenKlankeInput {
        val brukerInKlanke =
            klageLookupClient.getPerson(
                fnr = klanke.foedselsnummer,
                tema = innsendingsytelseToTema[klanke.innsendingsytelse],
            )
        return OpenKlankeInput(
            foedselsnummer = klanke.foedselsnummer,
            navn =
                Navn(
                    fornavn = brukerInKlanke.fornavn + if (brukerInKlanke.mellomnavn != null) " ${brukerInKlanke.mellomnavn}" else "",
                    etternavn = brukerInKlanke.etternavn,
                ),
            fritekst = klanke.fritekst ?: "",
            userSaksnummer = klanke.userSaksnummer,
            internalSaksnummer = klanke.sak?.fagsakid,
            vedtakDate = klanke.vedtakDate,
            innsendingsytelse = klanke.innsendingsytelse,
            language = klanke.language,
            hasVedlegg = klanke.vedlegg.isNotEmpty() || klanke.hasVedlegg,
            caseIsAtKA = klanke.caseIsAtKA,
            type = klanke.type,
            fullmektigFoedselsnummer = klanke.fullmektigFoedselsnummer,
        )
    }

    fun getKlanke(klankeId: UUID): KlankeView {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = klanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = klanke)
        return klanke.toKlankeView(
            userHasDocumentForThisTema =
                userHasDocumentForThisTema(
                    innsendingsytelse = klanke.innsendingsytelse,
                    userIdent = klanke.foedselsnummer,
                    documentCheckAction = DocumentCheckAction.OTHER,
                    type = klanke.type,
                ),
            fullmaktsgivere = getFullmaktsgivere(klanke.innsendingsytelse),
        )
    }

    fun validateAccess(klankeId: UUID) {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.validateKlankeAccess(klanke = klanke)
    }

    fun getJournalpostId(klankeId: UUID): String? {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = klanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = klanke)
        return klanke.journalpostId
    }

    fun updateFritekst(
        klankeId: UUID,
        fritekst: String,
    ): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.fritekst = fritekst
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateUserSaksnummer(
        klankeId: UUID,
        userSaksnummer: String?,
    ): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.userSaksnummer = userSaksnummer
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateVedtakDate(
        klankeId: UUID,
        vedtakDate: LocalDate?,
    ): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.vedtakDate = vedtakDate
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateCaseIsAtKA(
        klankeId: UUID,
        caseIsAtKA: Boolean,
    ): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.caseIsAtKA = caseIsAtKA
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateJournalpostIdWithoutValidation(
        klankeId: UUID,
        journalpostId: String,
    ): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()

        existingKlanke.journalpostId = journalpostId
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    private fun getAndValidateAccess(klankeId: UUID): Klanke {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke)
        return existingKlanke
    }

    fun updateHasVedlegg(
        klankeId: UUID,
        hasVedlegg: Boolean,
    ): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.hasVedlegg = hasVedlegg
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateStatusWithoutValidation(
        klankeId: UUID,
        status: KlageAnkeStatus,
    ): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()

        existingKlanke.status = status
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun deleteKlanke(klankeId: UUID) {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.status = KlageAnkeStatus.DELETED
        existingKlanke.modifiedByUser = LocalDateTime.now()
    }

    fun getJournalpostIdWithoutValidation(klankeId: UUID): String? {
        val klanke = klankeRepository.findJournalpostIdById(id = klankeId) ?: throw KlankeNotFoundException()
        return klanke.journalpostId
    }

    fun setJournalpostIdWithoutValidation(
        klankeId: UUID,
        journalpostId: String,
    ) {
        updateJournalpostIdWithoutValidation(klankeId, journalpostId)
        applicationEventPublisher.publishEvent(journalpostIdEvent(klankeId = klankeId, journalpostId = journalpostId))
    }

    fun setPdfDownloadedWithoutAccessValidation(
        klankeId: UUID,
        pdfDownloaded: LocalDateTime?,
    ) {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)

        existingKlanke.pdfDownloaded = pdfDownloaded
        existingKlanke.modifiedByUser = LocalDateTime.now()
    }

    private fun userHasDocumentForThisTema(
        innsendingsytelse: Innsendingsytelse,
        userIdent: String,
        documentCheckAction: DocumentCheckAction,
        type: Type,
    ): Boolean? {
        val temaForInnsendingsytelse = innsendingsytelseToTema[innsendingsytelse]!!

        val usersDocumentTemas =
            safSelvbetjeningService.getUsersDocumentTemas(
                userIdent = userIdent,
            ) ?: return null

        val userHasDocumentsForTema = usersDocumentTemas.contains(temaForInnsendingsytelse.name)

        if (!userHasDocumentsForTema) {
            when (documentCheckAction) {
                DocumentCheckAction.CREATE -> {
                    logger.info(
                        "Bruker opprettet ${type.name.lowercase()} på innsendingsytelse $innsendingsytelse, tema ${temaForInnsendingsytelse.name} uten å ha dokumenter i arkivet på temaet. Bruker har dokumenter på disse temaene: $usersDocumentTemas",
                    )
                    klageAnkeMetrics.incrementKlankerInitializedWithoutMatchingDocument(
                        innsendingsytelse = innsendingsytelse,
                        type = type,
                    )
                }

                DocumentCheckAction.FINALIZE -> {
                    logger.info(
                        "Bruker fullførte ${type.name.lowercase()} på innsendingsytelse $innsendingsytelse, tema ${temaForInnsendingsytelse.name} uten å ha dokumenter i arkivet på temaet. Bruker har dokumenter på disse temaene: $usersDocumentTemas",
                    )
                    klageAnkeMetrics.incrementKlankerFinalizedWithoutMatchingDocument(
                        innsendingsytelse = innsendingsytelse,
                        type = type,
                    )
                }

                else -> {}
            }
        }

        return userHasDocumentsForTema
    }

    enum class DocumentCheckAction {
        CREATE,
        FINALIZE,
        OTHER,
    }
}
