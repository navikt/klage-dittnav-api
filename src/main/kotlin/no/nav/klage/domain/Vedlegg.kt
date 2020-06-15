package no.nav.klage.domain

import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.klage.Klager
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.springframework.web.multipart.MultipartFile

data class VedleggWrapper(
    val content: MultipartFile,
    val tittel: String,
    val type: String
) {
    fun contentAsBytes(): ByteArray = content.bytes
}

data class Vedlegg(
    val tittel: String,
    val ref: String,
    val klageId: Int,
    val type: String = "Ukjent",
    val id: Int?,
    val sizeInBytes: Int
)

class VedleggDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VedleggDAO>(Vedleggene)

    var tittel by Vedleggene.tittel
    var ref by Vedleggene.ref
    var type by Vedleggene.type
    var sizeInBytes by Vedleggene.sizeInBytes
    var klageId by KlageDAO referencedOn Vedleggene.klageId

    fun toVedlegg(): Vedlegg =
        Vedlegg(
            tittel = tittel,
            ref = ref,
            klageId = klageId.id.value,
            id = id.value,
            type = type,
            sizeInBytes = sizeInBytes
        )
}

object Vedleggene : IntIdTable("vedlegg") {
    val tittel = varchar("tittel", 250)
    val ref = varchar("ref", 500)
    val klageId = reference("klage_id", Klager)
    val type = varchar("type", 10)
    val sizeInBytes = integer("size_in_bytes")
}
