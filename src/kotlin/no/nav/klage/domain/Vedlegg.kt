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
    var klageId by Vedleggene.klageId
}

object Vedleggene : IntIdTable("vedlegg") {
    var tittel = varchar("tittel", 250)
    var gcsRef = varchar("gcs_ref", 500)
    var klageId = integer("klageId")
}
