package no.nav.klage.domain.klage

import no.nav.klage.domain.Tema
import no.nav.klage.domain.vedlegg.VedleggDAO
import no.nav.klage.domain.vedlegg.Vedleggene
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant

class KlageDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<KlageDAO>(Klager)

    var foedselsnummer by Klager.foedselsnummer
    var fritekst by Klager.fritekst
    var status by Klager.status
    var modifiedByUser by Klager.modifiedByUser
    var tema by Klager.tema
    var vedtak by Klager.vedtak
    var saksnummer by Klager.saksnummer
    val vedlegg by VedleggDAO referrersOn Vedleggene.klageId
    var journalpostId by Klager.journalpostId
}

object Klager : IntIdTable("klage") {
    var foedselsnummer = varchar("foedselsnummer", 11)
    var fritekst = varchar("fritekst", 255)
    var status = varchar("status", 15)
    var modifiedByUser = timestamp("modified_by_user").default(Instant.now())
    var tema = varchar("tema", 3)
    var vedtak = varchar("vedtak", 100)
    var saksnummer = varchar("saksnummer", 25).nullable()
    var journalpostId = varchar("journalpost_id", 50).nullable()
}

fun KlageDAO.toKlage() =
    Klage(
        id = this.id.toString().toInt(),
        foedselsnummer = this.foedselsnummer,
        fritekst = this.fritekst,
        status = this.status.toStatus(),
        modifiedByUser = this.modifiedByUser,
        tema = this.tema.toTema(),
        vedtak = this.vedtak,
        saksnummer = this.saksnummer,
        vedlegg = this.vedlegg.map { it.toVedlegg() },
        journalpostId = this.journalpostId
    )

private fun String.toTema() = try {
    Tema.valueOf(this)
} catch (e: IllegalArgumentException) {
    Tema.UKJ
}

private fun String.toStatus() = try {
    KlageStatus.valueOf(this)
} catch (e: IllegalArgumentException) {
    KlageStatus.DRAFT
}