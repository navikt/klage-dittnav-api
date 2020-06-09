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
        val createdKlage = klageRepository.createKlage(klage)

        if (klage.status == DONE) {
            kafkaProducer.sendToKafka(createAggregatedKlage(bruker, createdKlage))
            klageMetrics.incrementKlagerCreated()
        }
        return createdKlage
    }

    fun updateKlage(klage: Klage): Klage {
        return klageRepository.updateKlage(klage)
    }

    fun deleteKlage(id: Int) {
        klageRepository.deleteKlage(id)
    }

}
