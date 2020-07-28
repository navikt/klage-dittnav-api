package no.nav.klage.service

import no.nav.klage.common.KlageMetrics
import no.nav.klage.common.VedleggMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.createAggregatedKlage
import no.nav.klage.domain.klage.KlageStatus.DONE
import no.nav.klage.domain.klage.KlageStatus.DRAFT
import no.nav.klage.domain.klage.KlageView
import no.nav.klage.domain.klage.toKlage
import no.nav.klage.domain.klage.toKlageView
import no.nav.klage.domain.klage.validateAccess
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
    private val kafkaProducer: KafkaProducer
) {

    fun getKlager(): List<KlageView> {
        return klageRepository.getKlager().map {
            it.toKlageView()
        }
    }

    fun getKlage(klageId: Int, bruker: Bruker): KlageView {
        val klage = klageRepository.getKlageById(klageId)
        klage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)
        return klage.toKlageView()
    }

    fun getJournalpostId(klageId: Int, bruker: Bruker): String? {
        return getKlage(klageId, bruker).journalpostId
    }

    fun createKlage(klage: KlageView, bruker: Bruker): KlageView {
        return klageRepository.createKlage(klage.toKlage(bruker, DRAFT)).toKlageView().also {
            klageMetrics.incrementKlagerInitialized()
        }
    }

    fun updateKlage(klage: KlageView, bruker: Bruker): KlageView {
        val klageId = klage.id
        checkNotNull(klageId) { "Klage is missing id" }
        val existingKlage = klageRepository.getKlageById(klageId)
        existingKlage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)

        return klageRepository.updateKlage(klage.toKlage(bruker)).toKlageView()
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
}
