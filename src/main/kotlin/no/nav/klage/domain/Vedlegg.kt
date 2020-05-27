package no.nav.klage.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.springframework.web.multipart.MultipartFile

data class VedleggWrapper(
    val content: MultipartFile,
    val tittel: String
) {
    fun contentAsBytes(): ByteArray = content.bytes
}

data class Vedlegg(
    val tittel: String,
    val gcsRef: String,
    val klageId: Int,
    val id: Int?
)

class VedleggDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VedleggDAO>(Vedleggene)

    var tittel by Vedleggene.tittel
    var gcsRef by Vedleggene.gcsRef
    var klageId by KlageDAO referencedOn Vedleggene.klageId

    fun toVedlegg(): Vedlegg =
        Vedlegg(
            tittel = tittel,
            gcsRef = gcsRef,
            klageId = klageId.id.value,
            id = id.value
        )
}

object Vedleggene : IntIdTable("vedlegg") {
    val tittel = varchar("tittel", 250)
    val gcsRef = varchar("gcs_ref", 500)
    val klageId = reference("klage_id", Klager)
}
