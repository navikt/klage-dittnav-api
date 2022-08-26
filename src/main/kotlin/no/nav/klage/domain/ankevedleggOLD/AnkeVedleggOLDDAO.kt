package no.nav.klage.domain.ankevedleggOLD

import no.nav.klage.domain.ankeOLD.AnkeOLDDAO
import no.nav.klage.domain.ankeOLD.Anker
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

class AnkeVedleggOLDDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AnkeVedleggOLDDAO>(AnkeVedleggene)

    var tittel by AnkeVedleggene.tittel
    var ref by AnkeVedleggene.ref
    var contentType by AnkeVedleggene.contentType
    var sizeInBytes by AnkeVedleggene.sizeInBytes
    var ankeId by AnkeOLDDAO referencedOn AnkeVedleggene.ankeId
    var ankeInternalSaksnummer by AnkeVedleggene.ankeInternalSaksnummer

    fun toAnkeVedlegg(): AnkeVedleggOLD =
        AnkeVedleggOLD(
            tittel = tittel,
            ref = ref,
            ankeInternalSaksnummer = ankeInternalSaksnummer.toString(),
            id = id.value,
            contentType = contentType,
            sizeInBytes = sizeInBytes

        )
}

object AnkeVedleggene : IntIdTable("anke_vedlegg_old") {
    val tittel = varchar("tittel", 250)
    val ref = varchar("ref", 500)
    val ankeId = reference("anke_id", Anker)
    val contentType = varchar("content_type", 50)
    val sizeInBytes = integer("size_in_bytes")
    val ankeInternalSaksnummer = uuid("anke_internal_saksnummer")
}