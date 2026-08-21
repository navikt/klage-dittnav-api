package no.nav.klage.service

import no.nav.klage.clients.klagelookup.KlageLookupClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.*
import no.nav.klage.domain.*
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.Sak
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.kodeverk.Tema
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToTema
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class CommonService(
    private val klankeRepository: KlankeRepository,
    private val validationService: ValidationService,
    private val kafkaInternalEventService: KafkaInternalEventService,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: AivenKafkaProducer,
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService,
    private val documentService: DocumentService,
    private val klageLookupClient: KlageLookupClient,
    private val tokenUtil: TokenUtil,
    private val safSelvbetjeningService: SafSelvbetjeningService,
) {

    companion object {

        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createKlanke(input: KlankeFullInput): KlankeView {
        val currentUser = tokenUtil.getSubject()
        val klanke = input.toKlanke(foedselsnummer = currentUser)
        return klankeRepository.save(klanke).also {
            updateMetrics(input = klanke)
        }.toKlankeView(userHasDocumentForThisTema = userHasDocumentForThisTema(klanke.innsendingsytelse))
    }

    private fun createKlanke(input: KlankeMinimalInput, foedselsnummer: String): Klanke {
        val klanke = input.toKlanke(foedselsnummer = foedselsnummer)
        return klankeRepository.save(klanke).also {
            updateMetrics(input = klanke)
        }
    }

    fun KlankeFullInput.toKlanke(foedselsnummer: String): Klanke {
        return Klanke(
            foedselsnummer = foedselsnummer,
            fritekst = fritekst,
            status = KlageAnkeStatus.DRAFT,
            userSaksnummer = userSaksnummer,
            journalpostId = null,
            vedtakDate = vedtakDate,
            sak = Sak(
                fagsakid = internalSaksnummer,
                sakstype = sakSakstype,
                fagsaksystem = sakFagsaksystem,
            ),
            language = language,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = hasVedlegg,
            created = LocalDateTime.now(),
            modifiedByUser = LocalDateTime.now(),
            pdfDownloaded = null,
            type = type,
            caseIsAtKA = caseIsAtKA,
        )
    }

    private fun KlankeMinimalInput.toKlanke(foedselsnummer: String): Klanke {
        return Klanke(
            foedselsnummer = foedselsnummer,
            fritekst = null,
            status = KlageAnkeStatus.DRAFT,
            userSaksnummer = null,
            journalpostId = null,
            vedtakDate = null,
            sak = Sak(
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
        )
    }

    private fun updateMetrics(input: Klanke) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(innsendingsytelse = input.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            innsendingsytelseToTema[input.innsendingsytelse]!!.name
        }
        klageAnkeMetrics.incrementKlankerInitialized(
            ytelse = temaReport,
            type = input.type
        )
    }

    fun getDraftOrCreateKlanke(input: KlankeMinimalInput): KlankeView {
        val currentUser = tokenUtil.getSubject()
        val existingKlanke = getLatestKlankeDraft(
            foedselsnummer = currentUser,
            internalSaksnummer = input.internalSaksnummer,
            innsendingsytelse = input.innsendingsytelse,
            type = input.type,
        )

        if (existingKlanke != null && input.caseIsAtKA != null) {
            existingKlanke.caseIsAtKA = input.caseIsAtKA
        }

        return existingKlanke?.toKlankeView(userHasDocumentForThisTema = userHasDocumentForThisTema(existingKlanke.innsendingsytelse)) ?: createKlanke(
            input = input,
            foedselsnummer = currentUser,
        ).toKlankeView(userHasDocumentForThisTema = userHasDocumentForThisTema(input.innsendingsytelse))
    }

    fun getLatestKlankeDraft(
        foedselsnummer: String,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse,
        type: Type,
    ): Klanke? {

        return klankeRepository.findByFoedselsnummerAndStatusAndType(
            fnr = foedselsnummer,
            status = KlageAnkeStatus.DRAFT,
            type = type
        )
            .filter {
                if (internalSaksnummer != null) {
                    it.innsendingsytelse == innsendingsytelse && it.sak?.fagsakid == internalSaksnummer
                } else {
                    it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser }
    }

    fun finalizeKlanke(klankeId: UUID): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)

        if (existingKlanke.isFinalized()) {
            return existingKlanke.modifiedByUser
        }

        validationService.validateKlankeAccess(
            klanke = existingKlanke,
        )
        validationService.validateKlanke(klanke = existingKlanke)

        existingKlanke.status = KlageAnkeStatus.DONE
        existingKlanke.modifiedByUser = LocalDateTime.now()

        kafkaProducer.sendToKafka(createAggregatedKlanke(klanke = existingKlanke))
        registerFinalizedMetrics(klanke = existingKlanke)

        logger.debug(
            "Klanke {} med innsendingsytelse {} er sendt inn.",
            klankeId,
            existingKlanke.innsendingsytelse.name,
        )

        return existingKlanke.modifiedByUser
    }

    private fun registerFinalizedMetrics(klanke: Klanke) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(klanke.innsendingsytelse)) {
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

        klageDittnavPdfgenService.createKlankePdfWithFoersteside(
            createPdfWithFoerstesideInput(klanke = existingKlanke)
        ).also {
            setPdfDownloadedWithoutAccessValidation(
                klankeId = klankeId,
                pdfDownloaded = LocalDateTime.now()
            )
            return it
        }
    }

    private fun createAggregatedKlanke(
        klanke: Klanke
    ): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(klanke.vedtakDate) ?: "Ikke angitt"
        val userInKlanke = klageLookupClient.getPerson(
            fnr = klanke.foedselsnummer,
            tema = innsendingsytelseToTema[klanke.innsendingsytelse]
        )

        return AggregatedKlageAnke(
            id = klanke.id.toString(),
            fornavn = userInKlanke.fornavn,
            mellomnavn = userInKlanke.mellomnavn ?: "",
            etternavn = userInKlanke.etternavn,
            vedtak = vedtak,
            dato = klanke.modifiedByUser.toLocalDate(),
            begrunnelse = sanitizeText(klanke.fritekst ?: ""),
            identifikasjonsnummer = klanke.foedselsnummer,
            ytelse = klanke.innsendingsytelse.nbName,
            vedlegg = klanke.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
            userSaksnummer = klanke.userSaksnummer,
            internalSaksnummer = klanke.sak?.fagsakid,
            klageAnkeType = AggregatedKlageAnke.KlageAnkeType.valueOf(klanke.type.name),
            ettersendelseTilKa = klanke.caseIsAtKA,
            innsendingsYtelseId = klanke.innsendingsytelse.id,
            sak = if (klanke.sak?.fagsakid != null && klanke.sak?.sakstype != null && klanke.sak?.fagsaksystem != null) {
                AggregatedKlageAnke.Sak(
                    sakstype = klanke.sak?.sakstype!!,
                    fagsaksystem = klanke.sak?.fagsaksystem!!,
                    fagsakid = klanke.sak?.fagsakid!!,
                )
            } else null
        )
    }

    fun createPdfWithFoerstesideInput(klanke: Klanke): OpenKlankeInput {
        val brukerInKlanke = klageLookupClient.getPerson(
            fnr = klanke.foedselsnummer,
            tema = innsendingsytelseToTema[klanke.innsendingsytelse]
        )
        return OpenKlankeInput(
            foedselsnummer = klanke.foedselsnummer,
            navn = Navn(
                fornavn = brukerInKlanke.fornavn,
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
        )
    }

    fun getKlanke(klankeId: UUID): KlankeView {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = klanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = klanke)
        return klanke.toKlankeView(userHasDocumentForThisTema = userHasDocumentForThisTema(klanke.innsendingsytelse))
    }

    fun validateAccess(klankeId: UUID) {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.validateKlankeAccess(klanke = klanke)
    }

    fun getJournalpostId(klankeId: UUID): String? {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke, false)
        validationService.validateKlankeAccess(klanke = klanke)
        return klanke.journalpostId
    }

    fun updateFritekst(klankeId: UUID, fritekst: String): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.fritekst = fritekst
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateUserSaksnummer(klankeId: UUID, userSaksnummer: String?): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.userSaksnummer = userSaksnummer
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateVedtakDate(klankeId: UUID, vedtakDate: LocalDate?): LocalDateTime {
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

    fun updateJournalpostIdWithoutValidation(klankeId: UUID, journalpostId: String): LocalDateTime {
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

    fun updateHasVedlegg(klankeId: UUID, hasVedlegg: Boolean): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId)

        existingKlanke.hasVedlegg = hasVedlegg
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateStatusWithoutValidation(klankeId: UUID, status: KlageAnkeStatus): LocalDateTime {
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
        val klanke = klankeRepository.findById(klankeId).get()
        return klanke.journalpostId
    }

    fun setJournalpostIdWithoutValidation(klankeId: UUID, journalpostId: String) {
        updateJournalpostIdWithoutValidation(klankeId, journalpostId)
        kafkaInternalEventService.publishEvent(
            Event(
                klageAnkeId = klankeId.toString(),
                name = "journalpostId",
                id = klankeId.toString(),
                data = journalpostId,
            )
        )
    }

    fun setPdfDownloadedWithoutAccessValidation(klankeId: UUID, pdfDownloaded: LocalDateTime?) {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)

        existingKlanke.pdfDownloaded = pdfDownloaded
        existingKlanke.modifiedByUser = LocalDateTime.now()
    }

    private fun userHasDocumentForThisTema(innsendingsytelse: Innsendingsytelse): Boolean {
        return safSelvbetjeningService.userHasDocumentForTema(innsendingsytelseToTema[innsendingsytelse]!!)
    }
}
