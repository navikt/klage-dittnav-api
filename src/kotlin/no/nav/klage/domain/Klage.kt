package no.nav.klage.domain

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable


data class Klage(
        var id: Int?,
        val klageId: Int,
        val foedselsnummer: String,
        val fritekst: String
)

class KlageDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<KlageDAO>(Klager)
    var klageId by Klager.klageId
    var foedselsnummer by Klager.foedselsnummer
    var fritekst by Klager.fritekst
}

object Klager : IntIdTable("klage") {
    val klageId = integer("klageid")
    val foedselsnummer = varchar("foedselsnummer", 11)
    val fritekst = varchar("fritekst", 255)
}
