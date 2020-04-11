package no.nav.klage.repository

import no.nav.klage.domain.Klage
import no.nav.klage.domain.Klager
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class KlageRepository {

    fun getKlager(): List<Klage> {
        return transaction {
            Klager.selectAll().map {
                it.toKlage()
            }
        }
    }

    fun getKlageById(id: Int): Klage {
        return transaction {
            Klager.select(Klager.id eq id).single().toKlage()
        }
    }

    fun getKlagerByKlageId(klageId: Int): List<Klage> {
        return transaction {
            Klager.select(Klager.klageId eq klageId).map { it.toKlage() }
        }
    }

    fun getKlagerByFnr(fnr: String): List<Klage> {
        return transaction {
            Klager.select(Klager.foedselsnummer eq fnr).map { it.toKlage() }
        }
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

    private fun ResultRow.toKlage() = Klage(
        id = this[Klager.id].toString().toInt(),
        klageId = this[Klager.klageId],
        foedselsnummer = this[Klager.foedselsnummer],
        fritekst = this[Klager.fritekst]
    )
}