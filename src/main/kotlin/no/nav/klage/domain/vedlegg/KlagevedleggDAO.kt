package no.nav.klage.domain.vedlegg

import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.klage.Klager
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

class KlagevedleggDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<KlagevedleggDAO>(Klagevedleggene)

    var tittel by Klagevedleggene.tittel
    var ref by Klagevedleggene.ref
    var contentType by Klagevedleggene.contentType
    var sizeInBytes by Klagevedleggene.sizeInBytes
    var klage by KlageDAO referencedOn Klagevedleggene.klageId

    fun toVedlegg(): Klagevedlegg =
        Klagevedlegg(
            tittel = tittel,
            ref = ref,
            klageId = klage.id.value.toString(),
            id = id.value,
            contentType = contentType,
            sizeInBytes = sizeInBytes
        )
}

object Klagevedleggene : IntIdTable("klage_vedlegg") {
    val tittel = varchar("tittel", 250)
    val ref = varchar("ref", 500)
    val klageId = reference("klage_id", Klager)
    val contentType = varchar("content_type", 50)
    val sizeInBytes = integer("size_in_bytes")
}
