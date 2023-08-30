package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.domain.*
import no.nav.klage.domain.exception.KlageIsFinalizedException
import no.nav.klage.domain.klage.*
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.repository.KlageRepository
import no.nav.klage.util.getLogger
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*
import java.time.ZoneOffset.UTC

@Service
@Transactional
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: AivenKafkaProducer,
    private val fileClient: FileClient,
    private val brukerService: BrukerService,
    private val validationService: ValidationService,
    private val kafkaInternalEventService: KafkaInternalEventService,
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService,
) {

    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getKlage(klageId: String, bruker: Bruker): KlageView {
        val klage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(klage, false)
        validationService.validateKlageAccess(klage, bruker)
        return klage.toKlageView(bruker = bruker)
    }

    fun validateAccess(klageId: String, bruker: Bruker) {
        val klage = klageRepository.getKlageById(klageId)
        validationService.validateKlageAccess(klage, bruker)
    }

    fun getDraftKlagerByFnr(bruker: Bruker): List<KlageView> {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
        val klager = klageRepository.getDraftKlagerByFnr(fnr)
        return klager.map { it.toKlageView(bruker) }
    }

    private fun getLatestDraftKlageByParams(
        bruker: Bruker,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse,
    ): KlageView? {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer

        val klage =
            klageRepository.getLatestKlageDraft(
                fnr = fnr,
                tema = tema,
                internalSaksnummer = internalSaksnummer,
                innsendingsytelse = innsendingsytelse,
            )
        if (klage != null) {
            validationService.validateKlageAccess(klage, bruker)
            return klage.toKlageView(bruker)
        }
        return null
    }

    fun getJournalpostId(klageId: String, bruker: Bruker): String? {
        val klage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(klage, false)
        validationService.validateKlageAccess(klage, bruker)
        return klage.journalpostId
    }

    private fun createKlageViewAndUpdateMetrics(input: Klage, bruker: Bruker): KlageView {
        return input.toKlageView(bruker)
            .also {
                val temaReport = if (klageAnkeIsLonnskompensasjon(innsendingsytelse = input.innsendingsytelse)) {
                    LOENNSKOMPENSASJON_GRAFANA_TEMA
                } else {
                    input.innsendingsytelse.toTema().toString()
                }
                klageAnkeMetrics.incrementKlagerInitialized(temaReport)
            }
    }

    fun createKlage(input: KlageFullInput, bruker: Bruker): KlageView {
        return createKlageViewAndUpdateMetrics(
            input = klageRepository.createKlage(klageFullInput = input, bruker = bruker),
            bruker = bruker
        )
    }

    fun createKlage(input: KlageInput, bruker: Bruker): KlageView {
        return createKlageViewAndUpdateMetrics(
            input = klageRepository.createKlage(klageInput = input, bruker = bruker),
            bruker = bruker
        )
    }

    fun getDraftOrCreateKlage(input: KlageInput, bruker: Bruker): KlageView {
        val existingKlage = getLatestDraftKlageByParams(
            bruker = bruker,
            tema = input.innsendingsytelse.toTema(),
            internalSaksnummer = input.internalSaksnummer,
            innsendingsytelse = input.innsendingsytelse,
        )

        return existingKlage ?: createKlage(
            input = input,
            bruker = bruker,
        )
    }

    fun updateFritekst(klageId: String, fritekst: String, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateFritekst(klageId, fritekst)
            .toKlageView(bruker)
            .modifiedByUser
    }

    fun updateUserSaksnummer(klageId: String, userSaksnummer: String?, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateUserSaksnummer(klageId, userSaksnummer)
            .toKlageView(bruker)
            .modifiedByUser
    }

    fun updateVedtakDate(klageId: String, vedtakDate: LocalDate?, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateVedtakDate(klageId, vedtakDate)
            .toKlageView(bruker)
            .modifiedByUser
    }

    fun updateHasVedlegg(klageId: String, hasVedlegg: Boolean, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateHasVedlegg(klageId, hasVedlegg)
            .toKlageView(bruker)
            .modifiedByUser
    }

    fun updateCheckboxesSelected(
        klageId: String,
        checkboxesSelected: Set<CheckboxEnum>?,
        bruker: Bruker
    ): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateCheckboxesSelected(klageId, checkboxesSelected)
            .toKlageView(bruker)
            .modifiedByUser
    }

    fun deleteKlage(klageId: String, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        klageRepository.updateStatus(klageId, KlageAnkeStatus.DELETED)
    }

    fun finalizeKlage(klageId: String, bruker: Bruker): Instant {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)

        if (existingKlage.isFinalized()) {
            return existingKlage.modifiedByUser
                ?: throw KlageIsFinalizedException("No modified date after finalize klage")
        }

        validationService.validateKlageAccess(existingKlage, bruker)
        validationService.validateKlage(existingKlage)

        val updatedKlage = klageRepository.updateStatus(klageId, KlageAnkeStatus.DONE)

        kafkaProducer.sendToKafka(createAggregatedKlage(bruker, updatedKlage))
        registerFinalizedMetrics(updatedKlage)

        logger.debug(
            "Klage {} med tema {} er sendt inn{}",
            klageId,
            existingKlage.tema.name,
            (if (existingKlage.fullmektig.isNullOrEmpty()) "." else " med fullmakt.")
        )

        return updatedKlage.modifiedByUser ?: throw KlageIsFinalizedException("No modified date after finalize klage")
    }

    fun getKlagePdf(klageId: String, bruker: Bruker): ByteArray {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        requireNotNull(existingKlage.journalpostId)
        return fileClient.getKlageAnkeFile(existingKlage.journalpostId)
    }

    fun createKlagePdfWithFoersteside(klageId: String, bruker: Bruker): ByteArray? {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        validationService.validateKlage(existingKlage)

        klageDittnavPdfgenService.createKlagePdfWithFoersteside(
            createPdfWithFoerstesideInput(existingKlage, bruker)
        ).also {
            setPdfDownloadedWithoutAccessValidation(klageId, Instant.now())
            return it
        }
    }

    fun getJournalpostIdWithoutValidation(klageId: String): String? {
        val klage = klageRepository.getKlageById(klageId)
        return klage.journalpostId
    }

    fun setJournalpostIdWithoutValidation(klageId: String, journalpostId: String) {
        klageRepository.updateJournalpostId(klageId, journalpostId)
        kafkaInternalEventService.publishEvent(
            Event(
                klageAnkeId = klageId,
                name = "journalpostId",
                id = klageId,
                data = journalpostId,
            )
        )
    }

    private fun setPdfDownloadedWithoutAccessValidation(klageId: String, pdfDownloaded: Instant?) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        klageRepository.updatePdfDownloaded(klageId, pdfDownloaded)
    }

    fun Klage.toKlageView(bruker: Bruker): KlageView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return KlageView(
            id = id.toString(),
            fritekst = fritekst ?: "",
            status = status,
            modifiedByUser = modifiedDateTime,
            vedlegg = vedlegg.map {
                it.toVedleggView()
            },
            journalpostId = journalpostId,
            finalizedDate = if (status === KlageAnkeStatus.DONE) modifiedDateTime.toLocalDate() else null,
            vedtakDate = vedtakDate,
            checkboxesSelected = checkboxesSelected ?: emptySet(),
            userSaksnummer = userSaksnummer,
            internalSaksnummer = internalSaksnummer,
            fullmaktsgiver = fullmektig?.let { foedselsnummer },
            language = language,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = hasVedlegg,
        )
    }

    private fun registerFinalizedMetrics(klage: Klage) {
        val temaReport = if (klageAnkeIsLonnskompensasjon(klage.innsendingsytelse)) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            klage.tema.toString()
        }
        klageAnkeMetrics.incrementKlagerFinalizedTitle(klage.innsendingsytelse)
        klageAnkeMetrics.incrementKlagerFinalized(temaReport)
        klageAnkeMetrics.incrementKlagerGrunn(temaReport, klage.checkboxesSelected ?: emptySet())
        if (klage.fullmektig != null) {
            klageAnkeMetrics.incrementFullmakt(temaReport)
        }
        if (klage.userSaksnummer != null) {
            klageAnkeMetrics.incrementOptionalSaksnummer(temaReport)
        }
        if (klage.vedtakDate != null) {
            klageAnkeMetrics.incrementOptionalVedtaksdato(temaReport)
        }
        vedleggMetrics.registerNumberOfVedleggPerUser(klage.vedlegg.size.toDouble())
    }

    private fun createAggregatedKlage(
        bruker: Bruker,
        klage: Klage
    ): AggregatedKlageAnke {
        val vedtak = vedtakFromDate(klage.vedtakDate)
        val fullmektigKlage = klage.fullmektig != null

        if (fullmektigKlage) {
            val fullmaktsGiver = brukerService.getFullmaktsgiver(klage.tema, klage.foedselsnummer)

            return AggregatedKlageAnke(
                id = klage.id.toString(),
                fornavn = fullmaktsGiver.navn.fornavn,
                mellomnavn = fullmaktsGiver.navn.mellomnavn ?: "",
                etternavn = fullmaktsGiver.navn.etternavn,
                vedtak = vedtak ?: "",
                dato = ZonedDateTime.ofInstant(klage.modifiedByUser, UTC).toLocalDate(),
                begrunnelse = sanitizeText(klage.fritekst!!),
                identifikasjonsnummer = fullmaktsGiver.folkeregisteridentifikator.identifikasjonsnummer,
                tema = klage.tema.name,
                ytelse = klage.innsendingsytelse.nb,
                vedlegg = klage.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
                userChoices = klage.checkboxesSelected?.map { x -> x.getFullText(klage.language) },
                userSaksnummer = klage.userSaksnummer,
                internalSaksnummer = klage.internalSaksnummer,
                klageAnkeType = AggregatedKlageAnke.KlageAnkeType.KLAGE,
                enhetsnummer = null,
            )
        } else {
            return AggregatedKlageAnke(
                id = klage.id.toString(),
                fornavn = bruker.navn.fornavn,
                mellomnavn = bruker.navn.mellomnavn ?: "",
                etternavn = bruker.navn.etternavn,
                vedtak = vedtak ?: "",
                dato = ZonedDateTime.ofInstant(klage.modifiedByUser, UTC).toLocalDate(),
                begrunnelse = sanitizeText(klage.fritekst!!),
                identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
                tema = klage.tema.name,
                ytelse = klage.innsendingsytelse.nb,
                vedlegg = klage.vedlegg.map { AggregatedKlageAnke.Vedlegg(tittel = it.tittel, ref = it.ref) },
                userChoices = klage.checkboxesSelected?.map { x -> x.getFullText(klage.language) },
                userSaksnummer = klage.userSaksnummer,
                internalSaksnummer = klage.internalSaksnummer,
                klageAnkeType = AggregatedKlageAnke.KlageAnkeType.KLAGE,
                enhetsnummer = null,
            )
        }
    }

    fun createPdfWithFoerstesideInput(klage: Klage, bruker: Bruker): OpenKlageInput {
        return OpenKlageInput(
            foedselsnummer = klage.foedselsnummer,
            navn = bruker.navn,
            fritekst = klage.fritekst!!,
            userSaksnummer = klage.userSaksnummer,
            internalSaksnummer = klage.internalSaksnummer,
            vedtakDate = klage.vedtakDate,
            innsendingsytelse = klage.innsendingsytelse,
            checkboxesSelected = klage.checkboxesSelected,
            language = klage.language,
            hasVedlegg = klage.vedlegg.isNotEmpty() || klage.hasVedlegg,
        )
    }
}
