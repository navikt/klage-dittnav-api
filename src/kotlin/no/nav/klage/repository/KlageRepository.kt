package no.nav.klage.repository

import no.nav.klage.domain.Klage
import no.nav.klage.domain.Klager
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class KlageRepository {

    fun getKlager(): List<Klage> {
        val klager = mutableListOf<Klage>()
        transaction {
            klager.addAll(Klager.selectAll().map {
                Klage(
                        id = it[Klager.id].toString().toInt(),
                        klageId = it[Klager.klageId],
                        foedselsnummer = it[Klager.foedselsnummer],
                        fritekst = it[Klager.fritekst]
                )
            })
        }
        return klager
    }

    fun addKlage(klage: Klage): Klage {
        transaction {
            klage.id = Klager.insertAndGetId {
                it[klageId] = klage.klageId
                it[foedselsnummer] = klage.foedselsnummer
                it[fritekst] = klage.fritekst
            }.toString().toInt()
        }
        return klage
    }
}