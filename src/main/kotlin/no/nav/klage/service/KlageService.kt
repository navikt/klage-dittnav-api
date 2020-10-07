package no.nav.klage.service

import no.nav.klage.clients.FileClient
import no.nav.klage.common.KlageMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.createAggregatedKlage
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.klage.KlageStatus.DONE
import no.nav.klage.domain.klage.KlageStatus.DRAFT
import no.nav.klage.domain.klage.KlageView
import no.nav.klage.domain.klage.toKlage
import no.nav.klage.domain.klage.validateAccess
import no.nav.klage.domain.vedlegg.toVedleggView
import no.nav.klage.kafka.KafkaProducer
import no.nav.klage.repository.KlageRepository
import no.nav.slackposter.Kibana
import no.nav.slackposter.SlackClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

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
        klage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)
        return klage.toKlageView(bruker)
    }

    fun getDraftKlagerByFnr(bruker: Bruker): List<KlageView> {
        val fnr = bruker.folkeregisteridentifikator.identifikasjonsnummer
        val klager = klageRepository.getDraftKlagerByFnr(fnr)
        return klager.map { it.toKlageView(bruker) }
    }

    fun getJournalpostId(klageId: Int, bruker: Bruker): String? {
        val klage = klageRepository.getKlageById(klageId)
        klage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer, false)
        return klage.journalpostId
    }

    fun setJournalpostId(klageId: Int, journalpostId: String) {
        val klage = klageRepository.getKlageById(klageId)
        val updatedKlage = klage.copy(journalpostId = journalpostId)
        klageRepository.updateKlage(updatedKlage)
    }

    fun createKlage(klage: KlageView, bruker: Bruker): KlageView {
        return klageRepository.createKlage(klage.toKlage(bruker, DRAFT)).toKlageView(bruker).also {
            klageMetrics.incrementKlagerInitialized()
            klageMetrics.incrementReferrer(if (klage.referrer.isNullOrBlank()) "none" else klage.referrer)
        }
    }

    fun updateKlage(klage: KlageView, bruker: Bruker) {
        val klageId = klage.id
        val existingKlage = klageRepository.getKlageById(klageId)
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)

        klageRepository.updateKlage(klage.toKlage(bruker)).toKlageView(bruker, false)
    }

    fun deleteKlage(klageId: Int, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)

        klageRepository.deleteKlage(klageId)
    }

    fun finalizeKlage(klageId: Int, bruker: Bruker): Instant {
        val existingKlage = klageRepository.getKlageById(klageId)
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)

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
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer, false)
        requireNotNull(existingKlage.journalpostId)
        return fileClient.getKlageFile(existingKlage.journalpostId)
    }

    fun Klage.toKlageView(bruker: Bruker, expandVedleggToVedleggView: Boolean = true) =
        KlageView(
            id!!,
            fritekst,
            tema,
            ytelse,
            vedtak,
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
            journalpostId
        )
}
