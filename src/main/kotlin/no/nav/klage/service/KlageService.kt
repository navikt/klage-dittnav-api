package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.exception.KlageIsFinalizedException
import no.nav.klage.domain.getCompoundedNavn
import no.nav.klage.domain.Tema
import no.nav.klage.domain.exception.KlageNotFoundException
import no.nav.klage.domain.klage.*
import no.nav.klage.domain.klage.KlageStatus.DONE
import no.nav.klage.domain.klage.KlageStatus.DRAFT
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.kafka.KafkaProducer
import no.nav.klage.repository.KlageRepository
import no.nav.klage.util.vedtakFromTypeAndDate
import no.nav.slackposter.Kibana
import no.nav.slackposter.SlackClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@Service
@Transactional
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageMetrics: KlageMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: KafkaProducer,
    private val vedleggService: VedleggService,
    private val slackClient: SlackClient,
    private val fileClient: FileClient,
    private val brukerService: BrukerService,
    private val validationService: ValidationService
) {

    fun getKlage(klageId: Int, bruker: Bruker): KlageView {
        val klage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(klage, false)
        validationService.validateAccess(klage, bruker)
        return klage.toKlageView(bruker, klage.status === DRAFT)
    }

    fun getDraftKlagerByFnr(bruker: Bruker): List<KlageView> {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
        val klager = klageRepository.getDraftKlagerByFnr(fnr)
        return klager.map { it.toKlageView(bruker) }
    }

    fun getLatestDraftKlageByParams(
        bruker: Bruker,
        tema: Tema,
        ytelse: String?,
        internalSaksnummer: String?,
        fullmaktsgiver: String?
    ): KlageView {
        val fnr = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer

        val klage =
            klageRepository.getLatestDraftKlageByFnrTemaYtelseInternalSaksnummer(
                fnr,
                tema,
                ytelse,
                internalSaksnummer
            )
        if (klage != null) {
            validationService.validateAccess(klage, bruker)
            return klage.toKlageView(bruker, false)
        }
        throw KlageNotFoundException()
    }

    fun getJournalpostId(klageId: Int, bruker: Bruker): String? {
        val klage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(klage, false)
        validationService.validateAccess(klage, bruker)
        return klage.journalpostId
    }

    fun createKlage(klage: KlageView, bruker: Bruker): KlageView {
        if (klage.fullmaktsgiver != null) {
            brukerService.verifyFullmakt(klage.tema, klage.fullmaktsgiver)
        }

        return klageRepository
            .createKlage(klage.toKlage(bruker, DRAFT))
            .toKlageView(bruker)
            .also {
                klageMetrics.incrementKlagerInitialized()
            }
    }

    fun updateKlage(klage: KlageView, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klage.id)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateAccess(existingKlage, bruker)
        klageRepository
            .updateKlage(klage.toKlage(bruker))
            .toKlageView(bruker, false)
    }

    fun deleteKlage(klageId: Int, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage)
        validationService.validateAccess(existingKlage, bruker)
        klageRepository.deleteKlage(klageId)
    }

    fun finalizeKlage(klageId: Int, bruker: Bruker): Instant {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)

        if (existingKlage.isFinalized()) {
            return existingKlage.modifiedByUser ?: throw KlageIsFinalizedException("No modified date after finalize klage")
        }

        validationService.validateAccess(existingKlage, bruker)
        existingKlage.status = DONE
        val updatedKlage = klageRepository.updateKlage(existingKlage)
        kafkaProducer.sendToKafka(createAggregatedKlage(bruker, updatedKlage))
        klageMetrics.incrementKlagerFinalized(updatedKlage.tema.toString())
        vedleggMetrics.registerNumberOfVedleggPerUser(updatedKlage.vedlegg.size.toDouble())

        val klageIdAsString = klageId.toString()
        slackClient.postMessage(
            String.format(
                "Klage med id <%s|%s> er sendt inn.",
                Kibana.createUrl(klageIdAsString),
                klageIdAsString
            )
        )

        return updatedKlage.modifiedByUser ?: throw KlageIsFinalizedException("No modified date after finalize klage")
    }

    fun getKlagePdf(klageId: Int, bruker: Bruker): ByteArray {
        val existingKlage = klageRepository.getKlageById(klageId)
        validationService.checkKlageStatus(existingKlage, false)
        validationService.validateAccess(existingKlage, bruker)
        requireNotNull(existingKlage.journalpostId)
        return fileClient.getKlageFile(existingKlage.journalpostId)
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
            ytelse,
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
            finalizedDate = if (status === DONE) modifiedDateTime.toLocalDate() else null,
            vedtakType = vedtakType,
            vedtakDate = vedtakDate,
            checkboxesSelected = checkboxesSelected ?: emptySet(),
            userSaksnummer = userSaksnummer,
            internalSaksnummer = internalSaksnummer,
            fullmaktsgiver = fullmektig?.let { foedselsnummer },
            language = language
        )
    }

    private fun createAggregatedKlage(
        bruker: Bruker,
        klage: Klage
    ): AggregatedKlage {
        val vedtak = vedtakFromTypeAndDate(klage.vedtakType, klage.vedtakDate)
        val fullmektigKlage = klage.fullmektig != null

        if (fullmektigKlage) {
            val fullmaktsGiver = brukerService.getFullmaktsgiver(klage.tema, klage.foedselsnummer)

            return AggregatedKlage(
                id = klage.id!!,
                klageInstans = false, // TODO: False for MVP
                trygderetten = false, // TODO: False for MVP
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
                ytelse = klage.ytelse,
                vedlegg = klage.vedlegg,
                userChoices = klage.checkboxesSelected?.map { x -> x.fullText },
                userSaksnummer = klage.userSaksnummer,
                internalSaksnummer = klage.internalSaksnummer,
                fullmektigNavn = bruker.getCompoundedNavn(),
                fullmektigFnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
            )
        } else {
            return AggregatedKlage(
                id = klage.id!!,
                klageInstans = false, // TODO: False for MVP
                trygderetten = false, // TODO: False for MVP
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
                ytelse = klage.ytelse,
                vedlegg = klage.vedlegg,
                userChoices = klage.checkboxesSelected?.map { x -> x.fullText },
                userSaksnummer = klage.userSaksnummer,
                internalSaksnummer = klage.internalSaksnummer,
                fullmektigNavn = null,
                fullmektigFnr = null
            )
        }
    }
}
