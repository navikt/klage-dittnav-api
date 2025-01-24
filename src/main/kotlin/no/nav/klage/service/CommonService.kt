package no.nav.klage.service

import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.KlankeFullInput
import no.nav.klage.controller.view.KlankeMinimalInput
import no.nav.klage.controller.view.OpenKlankeInput
import no.nav.klage.domain.*
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.domain.klage.AggregatedKlageAnke
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToTema
import no.nav.klage.repository.KlankeRepository
import no.nav.klage.util.getLogger
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
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
    private val documentService: DocumentService

) {

    companion object {

        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun createKlanke(input: KlankeFullInput, foedselsnummer: String): Klanke {
        val klanke = input.toKlanke(foedselsnummer = foedselsnummer)
        return klankeRepository.save(klanke).also {
            updateMetrics(input = klanke)
        }
    }

    fun createKlanke(input: KlankeMinimalInput, foedselsnummer: String): Klanke {
        val klanke = input.toKlanke(foedselsnummer = foedselsnummer)
        return klankeRepository.save(klanke).also {
            updateMetrics(input = klanke)
        }
    }

    fun KlankeFullInput.toKlanke(foedselsnummer: String): Klanke {
        return Klanke(
            checkboxesSelected = checkboxesSelected?.toMutableList() ?: mutableListOf(),
            foedselsnummer = foedselsnummer,
            fritekst = fritekst,
            status = KlageAnkeStatus.DRAFT,
            userSaksnummer = userSaksnummer,
            journalpostId = null,
            vedtakDate = vedtakDate,
            internalSaksnummer = internalSaksnummer,
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

    fun KlankeMinimalInput.toKlanke(foedselsnummer: String): Klanke {
        return Klanke(
            checkboxesSelected = mutableListOf(),
            foedselsnummer = foedselsnummer,
            fritekst = null,
            status = KlageAnkeStatus.DRAFT,
            userSaksnummer = null,
            journalpostId = null,
            vedtakDate = null,
            internalSaksnummer = internalSaksnummer,
            language = LanguageEnum.NB,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = false,
            created = LocalDateTime.now(),
            modifiedByUser = LocalDateTime.now(),
            pdfDownloaded = null,
            type = type,
            caseIsAtKA = null,
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

    fun getDraftOrCreateKlanke(input: KlankeMinimalInput, foedselsnummer: String): Klanke {
        val existingKlanke = getLatestKlankeDraft(
            foedselsnummer = foedselsnummer,
            internalSaksnummer = input.internalSaksnummer,
            innsendingsytelse = input.innsendingsytelse,
            type = input.type,
        )

        return existingKlanke ?: createKlanke(
            input = input,
            foedselsnummer = foedselsnummer,
        )
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
                    it.innsendingsytelse == innsendingsytelse && it.internalSaksnummer == internalSaksnummer
                } else {
                    it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser }
    }

    fun updateCheckboxesSelected(
        klankeId: UUID,
        checkboxesSelected: Set<CheckboxEnum>?,
        foedselsnummer: String,
    ): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(
            klanke = existingKlanke,
            foedselsnummer = foedselsnummer,
        )

        existingKlanke.checkboxesSelected.clear()
        if (!checkboxesSelected.isNullOrEmpty()) {
            existingKlanke.checkboxesSelected.addAll(checkboxesSelected)
        }
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun finalizeKlanke(klankeId: UUID, bruker: Bruker): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)

        if (existingKlanke.isFinalized()) {
            return existingKlanke.modifiedByUser
        }

        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer)
        validationService.validateKlanke(klanke = existingKlanke)

        existingKlanke.status = KlageAnkeStatus.DONE
        existingKlanke.modifiedByUser = LocalDateTime.now()

        kafkaProducer.sendToKafka(createAggregatedKlanke(bruker = bruker, klanke = existingKlanke))
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
            klageAnkeMetrics.incrementKlagerGrunn(temaReport, klanke.checkboxesSelected)
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

    fun getKlankePdf(klankeId: UUID, foedselsnummer: String): Pair<Path, String> {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = foedselsnummer)
        requireNotNull(existingKlanke.journalpostId)

        return documentService.getPathToDocumentPdfAndTitle(existingKlanke.journalpostId!!)
    }

    fun createKlankePdfWithFoersteside(klankeId: UUID, bruker: Bruker): ByteArray? {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer)

        validationService.validateKlanke(klanke = existingKlanke)

        klageDittnavPdfgenService.createKlankePdfWithFoersteside(
            createPdfWithFoerstesideInput(klanke = existingKlanke, bruker = bruker)
        ).also {
            setPdfDownloadedWithoutAccessValidation(
                klankeId = klankeId,
                pdfDownloaded = LocalDateTime.now()
            )
            return it
        }
    }

    private fun createAggregatedKlanke(
        bruker: Bruker,
        klanke: Klanke
    ): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(klanke.vedtakDate) ?: "Ikke angitt"

        return AggregatedKlageAnke(
            id = klanke.id.toString(),
            fornavn = bruker.navn.fornavn,
            mellomnavn = bruker.navn.mellomnavn ?: "",
            etternavn = bruker.navn.etternavn,
            vedtak = vedtak,
            dato = klanke.modifiedByUser.toLocalDate(),
            begrunnelse = sanitizeText(klanke.fritekst ?: ""),
            identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
            ytelse = klanke.innsendingsytelse.nbName,
            vedlegg = klanke.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
            userChoices = klanke.checkboxesSelected.map { x -> x.getFullText(klanke.language) },
            userSaksnummer = klanke.userSaksnummer,
            internalSaksnummer = klanke.internalSaksnummer,
            klageAnkeType = AggregatedKlageAnke.KlageAnkeType.valueOf(klanke.type.name),
            ettersendelseTilKa = klanke.caseIsAtKA,
            innsendingsYtelseId = klanke.innsendingsytelse.id,
        )
    }

    fun createPdfWithFoerstesideInput(klanke: Klanke, bruker: Bruker): OpenKlankeInput {
        return OpenKlankeInput(
            foedselsnummer = klanke.foedselsnummer,
            navn = bruker.navn,
            fritekst = klanke.fritekst ?: "",
            userSaksnummer = klanke.userSaksnummer,
            internalSaksnummer = klanke.internalSaksnummer,
            vedtakDate = klanke.vedtakDate,
            innsendingsytelse = klanke.innsendingsytelse,
            checkboxesSelected = klanke.checkboxesSelected.toSet(),
            language = klanke.language,
            hasVedlegg = klanke.vedlegg.isNotEmpty() || klanke.hasVedlegg,
            caseIsAtKA = klanke.caseIsAtKA,
            type = klanke.type,
        )
    }

    fun getKlanke(klankeId: UUID, foedselsnummer: String): Klanke {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = klanke, includeFinalized = false)
        validationService.validateKlankeAccess(klanke = klanke, foedselsnummer = foedselsnummer)
        return klanke
    }

    fun validateAccess(klankeId: UUID, foedselsnummer: String) {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.validateKlankeAccess(klanke = klanke, foedselsnummer = foedselsnummer)
    }

    fun getJournalpostId(klankeId: UUID, foedselsnummer: String): String? {
        val klanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke, false)
        validationService.validateKlankeAccess(klanke = klanke, foedselsnummer = foedselsnummer)
        return klanke.journalpostId
    }

    fun updateFritekst(klankeId: UUID, fritekst: String, foedselsnummer: String): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId, foedselsnummer = foedselsnummer)

        existingKlanke.fritekst = fritekst
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateUserSaksnummer(klankeId: UUID, userSaksnummer: String?, foedselsnummer: String): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId, foedselsnummer = foedselsnummer)

        existingKlanke.userSaksnummer = userSaksnummer
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateVedtakDate(klankeId: UUID, vedtakDate: LocalDate?, foedselsnummer: String): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId, foedselsnummer = foedselsnummer)

        existingKlanke.vedtakDate = vedtakDate
        existingKlanke.modifiedByUser = LocalDateTime.now()

        return existingKlanke.modifiedByUser
    }

    fun updateCaseIsAtKA(
        klankeId: UUID,
        caseIsAtKA: Boolean,
        foedselsnummer: String,
    ): LocalDateTime {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = foedselsnummer)

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

    private fun getAndValidateAccess(klankeId: UUID, foedselsnummer: String): Klanke {
        val existingKlanke = klankeRepository.findById(klankeId).get()
        validationService.checkKlankeStatus(klanke = existingKlanke)
        validationService.validateKlankeAccess(klanke = existingKlanke, foedselsnummer = foedselsnummer)
        return existingKlanke
    }

    fun updateHasVedlegg(klankeId: UUID, hasVedlegg: Boolean, foedselsnummer: String): LocalDateTime {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId, foedselsnummer = foedselsnummer)

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

    fun deleteKlanke(klankeId: UUID, foedselsnummer: String) {
        val existingKlanke = getAndValidateAccess(klankeId = klankeId, foedselsnummer = foedselsnummer)

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

}
