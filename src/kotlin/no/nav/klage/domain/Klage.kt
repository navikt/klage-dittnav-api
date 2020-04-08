package no.nav.klage.domain

import org.jetbrains.exposed.dao.IntIdTable


data class Klage(
        var id: Int?,
        val klageId: Int,
        val foedselsnummer: String,
        val fritekst: String
)

object Klager : IntIdTable("klage") {
    val klageId = integer("klageid")
    val foedselsnummer = varchar("foedselsnummer", 11)
    val fritekst = varchar("fritekst", 255)
}