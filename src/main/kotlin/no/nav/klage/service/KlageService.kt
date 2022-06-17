package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageAnkeMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.exception.KlageIsFinalizedException
import no.nav.klage.domain.getCompoundedNavn
import no.nav.klage.domain.klage.*
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.kafka.KafkaProducer
import no.nav.klage.repository.KlageRepository
import no.nav.klage.util.vedtakFromDate
import no.nav.slackposter.Kibana
import no.nav.slackposter.SlackClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@Service
@Transactional
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageAnkeMetrics: KlageAnkeMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: KafkaProducer,
    private val vedleggService: VedleggService,
    private val slackClient: SlackClient,
    private val fileClient: FileClient,
    private val brukerService: BrukerService,
    private val validationService: ValidationService
) {

    companion object {
        private const val LOENNSKOMPENSASJON_GRAFANA_TEMA = "LOK"
    }

    fun getKlage(klageId: Int, bruker: Bruker): KlageView {
        val klage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(klage, false)
        validationService.validateKlageAccess(klage, bruker)
        return klage.toKlageView(bruker, klage.status === KlageAnkeStatus.DRAFT)
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
        ytelse: String?
    ): KlageView? {
        val fnr = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer
        var processedTitleKey = titleKey
        if (ytelse == null && titleKey == null) {
            processedTitleKey = TitleEnum.valueOf(tema.name)
        } else if (ytelse != null && titleKey == null) {
            processedTitleKey = TitleEnum.getTitleKeyFromNbTitle(ytelse)
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

    fun updateKlage(klage: KlageView, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klage.id)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        klageRepository
            .updateKlage(klage.toKlage(bruker))
            .toKlageView(bruker, false)
    }

    fun updateFritekst(klageId: Int, fritekst: String, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        klageRepository
            .updateFritekst(klageId, fritekst)
            .toKlageView(bruker, false)
    }

    fun updateUserSaksnummer(klageId: Int, userSaksnummer: String?, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        klageRepository
            .updateUserSaksnummer(klageId, userSaksnummer)
            .toKlageView(bruker, false)
    }

    fun updateVedtakDate(klageId: Int, vedtakDate: LocalDate?, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        klageRepository
            .updateVedtakDate(klageId, vedtakDate)
            .toKlageView(bruker, false)
    }

    fun updateCheckboxesSelected(klageId: Int, checkboxesSelected: Set<CheckboxEnum>?, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateKlageAccess(existingKlage, bruker)
        klageRepository
            .updateCheckboxesSelected(klageId, checkboxesSelected)
            .toKlageView(bruker, false)
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
        existingKlage.status = KlageAnkeStatus.DONE
        val updatedKlage = klageRepository.updateKlage(existingKlage)
        kafkaProducer.sendToKafka(createAggregatedKlage(bruker, updatedKlage))
        registerFinalizedMetrics(updatedKlage)

        val klageIdAsString = klageId.toString()
        slackClient.postMessage(
            String.format(
                "Klage (<%s|%s>) med tema %s er sendt inn%s",
                Kibana.createUrl(klageIdAsString),
                klageIdAsString,
                existingKlage.tema.name,
                (if (existingKlage.fullmektig.isNullOrEmpty()) "." else " med fullmakt.")
            )
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

    fun getJournalpostIdWithoutValidation(klageId: Int): String? {
        val klage = klageRepository.getKlageById(klageId)
        return klage.journalpostId
    }

    fun setJournalpostIdWithoutValidation(klageId: Int, journalpostId: String) {
        val klage = klageRepository.getKlageById(klageId)
        val updatedKlage = klage.copy(journalpostId = journalpostId)
        klageRepository.updateKlage(updatedKlage, false)
    }

    fun Klage.toKlageView(bruker: Bruker, expandVedleggToVedleggView: Boolean = true): KlageView {
        val modifiedDateTime =
            ZonedDateTime.ofInstant((modifiedByUser ?: Instant.now()), ZoneId.of("Europe/Oslo")).toLocalDateTime()
        return KlageView(
            id!!,
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
            ytelse = titleKey.nb
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
                begrunnelse = klage.fritekst,
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
                begrunnelse = klage.fritekst,
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
}
