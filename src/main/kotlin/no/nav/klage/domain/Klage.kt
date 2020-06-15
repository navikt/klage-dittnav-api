package no.nav.klage.domain

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant
import java.time.LocalDate

data class Klage(
    val id: Int? = null,
    var foedselsnummer: String,
    val fritekst: String,
    var status: KlageStatus = KlageStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val enhetId: String? = null,
    val vedtaksdato: LocalDate,
    val referanse: String? = null,
    val vedlegg: List<Vedlegg> = listOf()
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
    var tema by Klager.tema
    var enhetId by Klager.enhetId
    var vedtaksdato by Klager.vedtaksdato
    var referanse by Klager.referanse
    val vedlegg by VedleggDAO referrersOn Vedleggene.klageId
}

object Klager : IntIdTable("klage") {
    var foedselsnummer = varchar("foedselsnummer", 11)
    var fritekst = varchar("fritekst", 255)
    var status = varchar("status", 15)
    var modifiedByUser = timestamp("modified_by_user").default(Instant.now())
    var tema = varchar("tema", 3)
    var enhetId = varchar("enhet_id", 4).nullable()
    var vedtaksdato = date("vedtaksdato")
    var referanse = varchar("referanse", 25).nullable()
}