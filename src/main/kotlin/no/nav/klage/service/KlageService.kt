package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.createAggregatedKlage
import no.nav.klage.domain.klage.*
import no.nav.klage.domain.klage.KlageStatus.DONE
import no.nav.klage.domain.klage.KlageStatus.DRAFT
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.kafka.KafkaProducer
import no.nav.klage.repository.KlageRepository
import no.nav.slackposter.Kibana
import no.nav.slackposter.SlackClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.ZoneId
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
    private val fileClient: FileClient
) {

    fun getKlage(klageId: Int, bruker: Bruker): KlageView {
        val klage = klageRepository.getKlageById(klageId)
        if (!klage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
        }
        return klage.toKlageView(bruker, klage.status === DRAFT)
    }

    fun getDraftKlagerByFnr(bruker: Bruker): List<KlageView> {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
        val klager = klageRepository.getDraftKlagerByFnr(fnr)
        return klager.map { it.toKlageView(bruker) }
    }

    fun getJournalpostId(klageId: Int, bruker: Bruker): String? {
        val klage = klageRepository.getKlageById(klageId)
        if (!klage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
        }
        return klage.journalpostId
    }

    fun createKlage(klage: KlageView, bruker: Bruker): KlageView {
        return klageRepository.createKlage(klage.toKlage(bruker, DRAFT)).toKlageView(bruker).also {
            klageMetrics.incrementKlagerInitialized()
        }
    }

    fun updateKlage(klage: KlageView, bruker: Bruker) {
        val klageId = klage.id
        val existingKlage = klageRepository.getKlageById(klageId)
        if (!existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
        }
        if (existingKlage.isFinalized()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Klage is already finalized.")
        }

        klageRepository.updateKlage(klage.toKlage(bruker)).toKlageView(bruker, false)
    }

    fun deleteKlage(klageId: Int, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        if (!existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
        }
        if (existingKlage.isFinalized()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Klage is already finalized.")
        }

        klageRepository.deleteKlage(klageId)
    }

    fun finalizeKlage(klageId: Int, bruker: Bruker): Instant {
        val existingKlage = klageRepository.getKlageById(klageId)
        if (!existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
        }

        if (existingKlage.isFinalized()) {
            return existingKlage.modifiedByUser ?: throw RuntimeException("No modified date after finalize klage")
        }

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

        return updatedKlage.modifiedByUser ?: throw RuntimeException("No modified date after finalize klage")
    }

    fun getKlagePdf(klageId: Int, bruker: Bruker): ByteArray {
        val existingKlage = klageRepository.getKlageById(klageId)
        if (!existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
        }
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
        klageRepository.updateKlage(updatedKlage)
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
            saksnummer,
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
            vedtakDate = vedtakDate
        )
    }
}
