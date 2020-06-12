package no.nav.klage.service

import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Klage
import no.nav.klage.domain.KlageStatus.DONE
import no.nav.klage.domain.createAggregatedKlage
import no.nav.klage.kafka.KafkaProducer
import no.nav.klage.repository.KlageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class KlageService(
    private val klageRepository: KlageRepository,
    private val klageMetrics: KlageMetrics,
    private val kafkaProducer: KafkaProducer
) {

    fun getKlager(): List<Klage> {
        return klageRepository.getKlager()
    }

    fun getKlage(id: Int): Klage = klageRepository.getKlageById(id)

    fun createKlage(klage: Klage, bruker: Bruker): Klage {
        klage.foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer
        return klageRepository.createKlage(klage)
    }

    fun updateKlage(klage: Klage): Klage {
        return klageRepository.updateKlage(klage)
    }

    fun deleteKlage(id: Int) {
        klageRepository.deleteKlage(id)
    }

    fun finalizeKlage(klageId: Int, bruker: Bruker) {
        val existingKlage = klageRepository.getKlageById(klageId)
        if (existingKlage.foedselsnummer != bruker.folkeregisteridentifikator.identifikasjonsnummer) {
            throw RuntimeException("Folkeregisteridentifikator in klage does not match current user.")
        }
        if (existingKlage.status === DONE) {
            throw RuntimeException("Klage is already finalized.")
        }
        existingKlage.status = DONE
        klageRepository.updateKlage(existingKlage)
        kafkaProducer.sendToKafka(createAggregatedKlage(bruker, existingKlage))
        klageMetrics.incrementKlagerCreated()
    }
}
