package no.nav.klage.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.domain.*
import no.nav.klage.domain.exception.KlageIsFinalizedException
import no.nav.klage.domain.klage.*
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.kafka.AivenKafkaProducer
import no.nav.klage.repository.KlageRepository
import no.nav.klage.util.getLogger
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
    private val vedleggService: VedleggService,
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
        val objectMapper: ObjectMapper = jacksonObjectMapper()
    }

    fun getKlage(klageId: Int, bruker: Bruker): KlageView {
        val klage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(klage, false)
        validationService.validateKlageAccess(klage, bruker)
        return klage.toKlageView(bruker, klage.status === KlageAnkeStatus.DRAFT)
    }

    fun validateAccess(klageId: Int, bruker: Bruker) {
        val klage = klageRepository.getKlageById(klageId)
        validationService.validateKlageAccess(klage, bruker)
    }

    fun getDraftKlagerByFnr(bruker: Bruker): List<KlageView> {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
        val klager = klageRepository.getDraftKlagerByFnr(fnr)
        return klager.map { it.toKlageView(bruker) }
    }

    fun getLatestDraftKlageByParams(
        bruker: Bruker,
        tema: Tema,
        internalSaksnummer: String?,
        fullmaktsgiver: String?,
        titleKey: TitleEnum?,
    ): KlageView? {
        val fnr = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer
        var processedTitleKey = titleKey
        if (titleKey == null) {
            processedTitleKey = TitleEnum.valueOf(tema.name)
        }

        val klage =
            klageRepository.getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
                fnr,
                tema,
                internalSaksnummer,
                processedTitleKey
            )
        if (klage != null) {
            validationService.validateKlageAccess(klage, bruker)
            return klage.toKlageView(bruker, true)
        }
        return null
    }

    fun getJournalpostId(klageId: Int, bruker: Bruker): String? {
        val klage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(klage, false)
        validationService.validateKlageAccess(klage, bruker)
        return klage.journalpostId
    }

    fun createKlage(klage: KlageView, bruker: Bruker): KlageView {
        if (klage.fullmaktsgiver != null) {
            brukerService.verifyFullmakt(klage.tema, klage.fullmaktsgiver)
        }

        return klageRepository
            .createKlage(klage.toKlage(bruker, KlageAnkeStatus.DRAFT))
            .toKlageView(bruker)
            .also {
                val temaReport = if (klage.isLonnskompensasjon()) {
                    LOENNSKOMPENSASJON_GRAFANA_TEMA
                } else {
                    klage.tema.toString()
                }
                klageAnkeMetrics.incrementKlagerInitialized(temaReport)
            }
    }

    fun createKlage(input: KlageInput, bruker: Bruker): KlageView {
        if (input.fullmaktsgiver != null) {
            brukerService.verifyFullmakt(input.tema, input.fullmaktsgiver)
        }

        return klageRepository
            .createKlage(input.toKlage(bruker))
            .toKlageView(bruker)
            .also {
                val temaReport = if (input.isLonnskompensasjon()) {
                    LOENNSKOMPENSASJON_GRAFANA_TEMA
                } else {
                    input.tema.toString()
                }
                klageAnkeMetrics.incrementKlagerInitialized(temaReport)
            }
    }

    fun getDraftOrCreateKlage(input: KlageInput, bruker: Bruker): KlageView {
        val existingKlage = getLatestDraftKlageByParams(
            bruker = bruker,
            tema = input.tema,
            internalSaksnummer = input.internalSaksnummer,
            fullmaktsgiver = input.fullmaktsgiver,
            titleKey = input.titleKey,
        )

        return existingKlage ?: createKlage(
            input = input,
            bruker = bruker,
        )
    }

    fun updateFritekst(klageId: String, fritekst: String, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId.toInt())
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateFritekst(klageId, fritekst)
            .toKlageView(bruker, false)
            .modifiedByUser
    }

    fun updateUserSaksnummer(klageId: String, userSaksnummer: String?, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId.toInt())
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateUserSaksnummer(klageId, userSaksnummer)
            .toKlageView(bruker, false)
            .modifiedByUser
    }

    fun updateVedtakDate(klageId: String, vedtakDate: LocalDate?, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId.toInt())
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateVedtakDate(klageId, vedtakDate)
            .toKlageView(bruker, false)
            .modifiedByUser
    }

    fun updateHasVedlegg(klageId: String, hasVedlegg: Boolean, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId.toInt())
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateHasVedlegg(klageId, hasVedlegg)
            .toKlageView(bruker, false)
            .modifiedByUser
    }

    fun updateCheckboxesSelected(klageId: String, checkboxesSelected: Set<CheckboxEnum>?, bruker: Bruker): LocalDateTime {
        val existingKlage = klageRepository.getKlageById(klageId.toInt())
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        return klageRepository
            .updateCheckboxesSelected(klageId, checkboxesSelected)
            .toKlageView(bruker, false)
            .modifiedByUser
    }

    fun deleteKlage(klageId: Int, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        klageRepository.deleteKlage(klageId)
    }

    fun finalizeKlage(klageId: Int, bruker: Bruker): Instant {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)

        if (existingKlage.isFinalized()) {
            return existingKlage.modifiedByUser
                ?: throw KlageIsFinalizedException("No modified date after finalize klage")
        }

        validationService.validateKlageAccess(existingKlage, bruker)
        validationService.validateKlage(existingKlage)
        existingKlage.status = KlageAnkeStatus.DONE
        val updatedKlage = klageRepository.updateKlage(existingKlage)
        kafkaProducer.sendToKafka(createAggregatedKlage(bruker, updatedKlage))
        registerFinalizedMetrics(updatedKlage)

        val klageIdAsString = klageId.toString()
        logger.debug(
            "Klage {} med tema {} er sendt inn{}",
            klageIdAsString,
            existingKlage.tema.name,
            (if (existingKlage.fullmektig.isNullOrEmpty()) "." else " med fullmakt.")
        )

        return updatedKlage.modifiedByUser ?: throw KlageIsFinalizedException("No modified date after finalize klage")
    }

    fun getKlagePdf(klageId: Int, bruker: Bruker): ByteArray {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        requireNotNull(existingKlage.journalpostId)
        return fileClient.getKlageAnkeFile(existingKlage.journalpostId)
    }

    fun createKlagePdfWithFoersteside(klageId: Int, bruker: Bruker): ByteArray? {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateKlageAccess(existingKlage, bruker)
        validationService.validateKlage(existingKlage)

        return klageDittnavPdfgenService.createKlagePdfWithFoersteside(
            createPdfWithFoerstesideInput(existingKlage, bruker)
        )
    }

    fun getJournalpostIdWithoutValidation(klageId: Int): String? {
        val klage = klageRepository.getKlageById(klageId)
        return klage.journalpostId
    }

    fun setJournalpostIdWithoutValidation(klageId: Int, journalpostId: String) {
        val klage = klageRepository.getKlageById(klageId)
        val updatedKlage = klage.copy(journalpostId = journalpostId)
        klageRepository.updateKlage(updatedKlage, false)
        kafkaInternalEventService.publishEvent(
            Event(
                klageId = klageId.toString(),
                name = "journalpostId",
                id = klageId.toString(),
                data = journalpostId,
            )
        )
    }

    fun Klage.toKlageView(bruker: Bruker, expandVedleggToVedleggView: Boolean = true): KlageView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return KlageView(
            id!!.toString(),
            fritekst,
            tema,
            status,
            modifiedDateTime,
            vedlegg.map {
                if (expandVedleggToVedleggView) {
                    vedleggService.expandVedleggToVedleggView(
                        it,
                        bruker
                    )
                } else {
                    it.toVedleggView("")
                }
            },
            journalpostId,
            finalizedDate = if (status === KlageAnkeStatus.DONE) modifiedDateTime.toLocalDate() else null,
            vedtakDate = vedtakDate,
            checkboxesSelected = checkboxesSelected ?: emptySet(),
            userSaksnummer = userSaksnummer,
            internalSaksnummer = internalSaksnummer,
            fullmaktsgiver = fullmektig?.let { foedselsnummer },
            language = language,
            titleKey = titleKey,
            ytelse = titleKey.nb,
            hasVedlegg = hasVedlegg,
        )
    }

    private fun registerFinalizedMetrics(klage: Klage) {
        val temaReport = if (klage.isLonnskompensasjon()) {
            LOENNSKOMPENSASJON_GRAFANA_TEMA
        } else {
            klage.tema.toString()
        }
        klageAnkeMetrics.incrementKlagerFinalizedTitle(klage.titleKey)
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
    ): AggregatedKlage {
        val vedtak = vedtakFromDate(klage.vedtakDate)
        val fullmektigKlage = klage.fullmektig != null

        if (fullmektigKlage) {
            val fullmaktsGiver = brukerService.getFullmaktsgiver(klage.tema, klage.foedselsnummer)

            return AggregatedKlage(
                id = klage.id!!,
                fornavn = fullmaktsGiver.navn.fornavn,
                mellomnavn = fullmaktsGiver.navn.mellomnavn ?: "",
                etternavn = fullmaktsGiver.navn.etternavn,
                adresse = fullmaktsGiver.adresse?.toKlageskjemaString() ?: "Ukjent adresse",
                telefon = bruker.kontaktinformasjon?.telefonnummer ?: "",
                vedtak = vedtak ?: "",
                dato = ZonedDateTime.ofInstant(klage.modifiedByUser, UTC).toLocalDate(),
                begrunnelse = sanitizeText(klage.fritekst!!),
                identifikasjonstype = fullmaktsGiver.folkeregisteridentifikator.type,
                identifikasjonsnummer = fullmaktsGiver.folkeregisteridentifikator.identifikasjonsnummer,
                tema = klage.tema.name,
                ytelse = klage.titleKey.nb,
                vedlegg = klage.vedlegg,
                userChoices = klage.checkboxesSelected?.map { x -> x.getFullText(klage.language) },
                userSaksnummer = klage.userSaksnummer,
                internalSaksnummer = klage.internalSaksnummer,
                fullmektigNavn = bruker.getCompoundedNavn(),
                fullmektigFnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
            )
        } else {
            return AggregatedKlage(
                id = klage.id!!,
                fornavn = bruker.navn.fornavn,
                mellomnavn = bruker.navn.mellomnavn ?: "",
                etternavn = bruker.navn.etternavn,
                adresse = bruker.adresse?.toKlageskjemaString() ?: "Ukjent adresse",
                telefon = bruker.kontaktinformasjon?.telefonnummer ?: "",
                vedtak = vedtak ?: "",
                dato = ZonedDateTime.ofInstant(klage.modifiedByUser, UTC).toLocalDate(),
                begrunnelse = sanitizeText(klage.fritekst!!),
                identifikasjonstype = bruker.folkeregisteridentifikator.type,
                identifikasjonsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
                tema = klage.tema.name,
                ytelse = klage.titleKey.nb,
                vedlegg = klage.vedlegg,
                userChoices = klage.checkboxesSelected?.map { x -> x.getFullText(klage.language) },
                userSaksnummer = klage.userSaksnummer,
                internalSaksnummer = klage.internalSaksnummer,
                fullmektigNavn = null,
                fullmektigFnr = null
            )
        }
    }

    fun createPdfWithFoerstesideInput(klage: Klage, bruker: Bruker): OpenKlageInput {
        return OpenKlageInput(
            foedselsnummer = klage.foedselsnummer,
            navn = bruker.navn,
            fritekst = klage.fritekst!!,
            userSaksnummer = klage.userSaksnummer,
            vedtakDate = klage.vedtakDate,
            titleKey = klage.titleKey,
            tema = klage.tema,
            checkboxesSelected = klage.checkboxesSelected,
            language = klage.language,
            hasVedlegg = klage.vedlegg.isNotEmpty() || klage.hasVedlegg,
        )
    }
}
