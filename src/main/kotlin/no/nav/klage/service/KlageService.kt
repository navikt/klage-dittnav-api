package no.nav.klage.service

import no.nav.klage.common.KlageMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.JournalpostStatus
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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageMetrics: KlageMetrics,
    private val vedleggMetrics: VedleggMetrics,
    private val kafkaProducer: KafkaProducer,
    private val vedleggService: VedleggService
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

    fun getJournalpostStatus(klageId: Int, bruker: Bruker): JournalpostStatus {
        val klage = klageRepository.getKlageById(klageId)
        klage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer, false)
        return klage.journalpostStatus
    }

    fun setJournalpostId(klageId: Int, journalpostId: String) {
        val klage = klageRepository.getKlageById(klageId)
        val updatedKlage = klage.copy(journalpostId = journalpostId)
        klageRepository.updateKlage(updatedKlage)
    }

    fun setJournalpostStatus(journalpostId: String, journalpostStatus: JournalpostStatus) {
        val klage = klageRepository.getKlageByJournalpostId(journalpostId)
        val updatedKlage = klage.copy(journalpostStatus = journalpostStatus)
        klageRepository.updateKlage(updatedKlage)
    }

    fun createKlage(klage: KlageView, bruker: Bruker): KlageView {
        return klageRepository.createKlage(klage.toKlage(bruker, DRAFT)).toKlageView(bruker).also {
            klageMetrics.incrementKlagerInitialized()
        }
    }

    fun updateKlage(klage: KlageView, bruker: Bruker): KlageView {
        val klageId = klage.id
        val existingKlage = klageRepository.getKlageById(klageId)
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)

        return klageRepository.updateKlage(klage.toKlage(bruker)).toKlageView(bruker, false)
    }

    fun deleteKlage(klageId: Int, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)

        klageRepository.deleteKlage(klageId)
    }

    fun finalizeKlage(klageId: Int, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)

        existingKlage.status = DONE
        klageRepository.updateKlage(existingKlage)
        kafkaProducer.sendToKafka(createAggregatedKlage(bruker, existingKlage))
        klageMetrics.incrementKlagerFinalized(existingKlage.ytelse)
        vedleggMetrics.registerNumberOfVedleggPerUser(existingKlage.vedlegg.size.toDouble())
    }

    fun Klage.toKlageView(bruker: Bruker, expandVedleggToVedleggView: Boolean = true) =
        KlageView(
            id!!,
            fritekst,
            tema,
            ytelse,
            enhetId,
            vedtaksdato,
            referanse,
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
            journalpostStatus
        )
}
