package no.nav.klage.domain.klage

import no.nav.klage.domain.Tema
import no.nav.klage.domain.vedlegg.VedleggDAO
import no.nav.klage.domain.vedlegg.Vedleggene
import no.nav.klage.util.vedtakDateFromVedtak
import no.nav.klage.util.vedtakTypeFromVedtak
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant

class KlageDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<KlageDAO>(Klager)

    var foedselsnummer by Klager.foedselsnummer
    var fritekst by Klager.fritekst
    var status by Klager.status
    var modifiedByUser by Klager.modifiedByUser
    var tema by Klager.tema
    var ytelse by Klager.ytelse
    var vedtak by Klager.vedtak
    var saksnummer by Klager.saksnummer
    val vedlegg by VedleggDAO referrersOn Vedleggene.klageId
    var journalpostId by Klager.journalpostId
    var vedtakType by Klager.vedtakType
    var vedtakDate by Klager.vedtakDate
    var checkBoxesSelected by Klager.checkboxesSelected
}

object Klager : IntIdTable("klage") {
    var foedselsnummer = varchar("foedselsnummer", 11)
    var fritekst = text("fritekst")
    var status = varchar("status", 15)
    var modifiedByUser = timestamp("modified_by_user").default(Instant.now())
    var tema = varchar("tema", 3)
    var ytelse = varchar("ytelse", 300)
    var vedtak = varchar("vedtak", 100).nullable()
    var saksnummer = varchar("saksnummer", 25).nullable()
    var journalpostId = varchar("journalpost_id", 50).nullable()
    var vedtakType = varchar("vedtak_type", 25).nullable()
    var vedtakDate = date("vedtak_date").nullable()
    var checkboxesSelected = text("checkboxes_selected").nullable()
}

fun KlageDAO.toKlage(): Klage {
    var outputKlage = Klage(
        id = this.id.toString().toInt(),
        foedselsnummer = this.foedselsnummer,
        fritekst = this.fritekst,
        status = this.status.toStatus(),
        modifiedByUser = this.modifiedByUser,
        tema = this.tema.toTema(),
        ytelse = this.ytelse,
        saksnummer = this.saksnummer,
        vedlegg = this.vedlegg.map { it.toVedlegg() },
        journalpostId = this.journalpostId,
        vedtakType = this.vedtakType.toVedtakType(),
        vedtakDate = this.vedtakDate,
        checkboxesSelected = this.checkBoxesSelected?.toCheckboxEnumSet()
    )

    outputKlage = if (this.vedtak != null && (this.vedtakType == null && this.vedtakDate == null)) {
        outputKlage.copy(
            vedtakType = vedtakTypeFromVedtak(this.vedtak!!),
            vedtakDate = vedtakDateFromVedtak(this.vedtak!!)
        )
    } else {
        outputKlage
    }

    return outputKlage
}

fun String.toTema() = try {
    Tema.valueOf(this)
} catch (e: IllegalArgumentException) {
    Tema.UKJ
}

fun String.toStatus() = try {
    KlageStatus.valueOf(this)
} catch (e: IllegalArgumentException) {
    KlageStatus.DRAFT
}

fun String?.toVedtakType() =
    if (this != null) VedtakType.valueOf(this) else null

fun String.toCheckboxEnumSet() =
    if (this == "") {
        emptySet()
    } else {
        this.split(",").map { x -> CheckboxEnum.valueOf(x) }.toSet()
    }

fun KlageDAO.fromKlage(klage: Klage) {
    foedselsnummer = klage.foedselsnummer
    fritekst = klage.fritekst
    status = klage.status.name
    modifiedByUser = Instant.now()
    tema = klage.tema.name
    ytelse = klage.ytelse
    vedtak = null
    klage.saksnummer?.let { saksnummer = it }
    klage.journalpostId?.let { journalpostId = it }
    vedtakType = klage.vedtakType?.name
    vedtakDate = klage.vedtakDate
    klage.checkboxesSelected?.let { checkBoxesSelected = it.joinToString(",") { x -> x.toString() } }
}