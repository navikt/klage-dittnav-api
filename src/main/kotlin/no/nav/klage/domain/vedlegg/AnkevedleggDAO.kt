package no.nav.klage.domain.vedlegg

import no.nav.klage.domain.anke.AnkeDAO
import no.nav.klage.domain.anke.Anker
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

class AnkevedleggDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AnkevedleggDAO>(Ankevedleggene)

    var tittel by Ankevedleggene.tittel
    var ref by Ankevedleggene.ref
    var contentType by Ankevedleggene.contentType
    var sizeInBytes by Ankevedleggene.sizeInBytes
    var anke by AnkeDAO referencedOn Ankevedleggene.ankeId

    fun toVedlegg(): Ankevedlegg =
        Ankevedlegg(
            tittel = tittel,
            ref = ref,
            ankeId = anke.id.value,
            id = id.value,
            contentType = contentType,
            sizeInBytes = sizeInBytes
        )
}

object Ankevedleggene : IntIdTable("anke_vedlegg") {
    val tittel = varchar("tittel", 250)
    val ref = varchar("ref", 500)
    val ankeId = reference("anke_id", Anker)
    val contentType = varchar("content_type", 50)
    val sizeInBytes = integer("size_in_bytes")
}
