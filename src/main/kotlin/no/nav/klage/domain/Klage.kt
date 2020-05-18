package no.nav.klage.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.postgresql.util.PGobject
import java.time.Instant

data class Klage(
    val id: Int?,
    val foedselsnummer: String,
    val fritekst: String,
    val status: KlageStatus = KlageStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now()
)

enum class KlageStatus {
    DRAFT, DONE, DELETED
}

class KlageDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<KlageDAO>(Klager)

    var foedselsnummer by Klager.foedselsnummer
    var fritekst by Klager.fritekst
    var status by Klager.status
    var modifiedByUser by Klager.modifiedByUser
}

object Klager : IntIdTable("klage") {
    var foedselsnummer = varchar("foedselsnummer", 11)
    var fritekst = varchar("fritekst", 255)
    var status = customEnumeration(
        name = "status",
        fromDb = { value -> KlageStatus.valueOf(value as String) },
        toDb = { PGEnum("KlageStatusType", it) })
    var modifiedByUser = timestamp("modifiedbyuser").default(Instant.now())
}

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}
