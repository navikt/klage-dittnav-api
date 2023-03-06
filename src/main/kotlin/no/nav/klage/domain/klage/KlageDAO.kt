package no.nav.klage.domain.klage

import no.nav.klage.domain.vedlegg.KlagevedleggDAO
import no.nav.klage.domain.vedlegg.Klagevedleggene
import no.nav.klage.util.getLanguageEnum
import no.nav.klage.util.getTitleEnum
import no.nav.klage.util.toStatus
import no.nav.klage.util.toTema
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

class KlageDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<KlageDAO>(Klager)

    var foedselsnummer by Klager.foedselsnummer
    var fritekst by Klager.fritekst
    var status by Klager.status
    var modifiedByUser by Klager.modifiedByUser
    var tema by Klager.tema
    var ytelse by Klager.ytelse
    var userSaksnummer by Klager.userSaksnummer
    val vedlegg by KlagevedleggDAO referrersOn Klagevedleggene.klageId
    var journalpostId by Klager.journalpostId
    var vedtakDate by Klager.vedtakDate
    var checkBoxesSelected by Klager.checkboxesSelected
    var internalSaksnummer by Klager.internalSaksnummer
    var fullmektig by Klager.fullmektig
    var language by Klager.language
    var titleKey by Klager.titleKey
    var hasVedlegg by Klager.hasVedlegg
    var pdfDownloaded by Klager.pdfDownloaded
}

object Klager : IntIdTable("klage") {
    var foedselsnummer = varchar("foedselsnummer", 11)
    var fritekst = text("fritekst").nullable()
    var status = varchar("status", 15)
    var modifiedByUser = timestamp("modified_by_user").default(Instant.now())
    var tema = varchar("tema", 3)
    var ytelse = varchar("ytelse", 300).nullable()
    var userSaksnummer = text("user_saksnummer").nullable()
    var journalpostId = varchar("journalpost_id", 50).nullable()
    var vedtakDate = date("vedtak_date").nullable()
    var checkboxesSelected = text("checkboxes_selected").nullable()
    var internalSaksnummer = text("internal_saksnummer").nullable()
    var fullmektig = varchar("fullmektig", 11).nullable()
    var language = text("language").nullable()
    var titleKey = text("title_key").nullable()
    var hasVedlegg = bool("has_vedlegg").default(false)
    var pdfDownloaded = timestamp("pdf_downloaded").nullable()
}

fun KlageDAO.toKlage(): Klage {
    return Klage(
        id = id.toString().toInt(),
        foedselsnummer = foedselsnummer,
        fritekst = fritekst,
        status = status.toStatus(),
        modifiedByUser = modifiedByUser,
        tema = tema.toTema(),
        userSaksnummer = userSaksnummer,
        vedlegg = vedlegg.map { it.toVedlegg() },
        journalpostId = journalpostId,
        vedtakDate = vedtakDate,
        checkboxesSelected = checkBoxesSelected?.toCheckboxEnumSet(),
        internalSaksnummer = internalSaksnummer,
        fullmektig = fullmektig,
        language = getLanguageEnum(this.language),
        titleKey = getTitleEnum(this.titleKey, this.ytelse, this.tema),
        hasVedlegg = hasVedlegg,
    )
}

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
    userSaksnummer = klage.userSaksnummer
    klage.journalpostId?.let { journalpostId = it }
    vedtakDate = klage.vedtakDate
    klage.checkboxesSelected?.let { checkBoxesSelected = it.joinToString(",") { x -> x.toString() } }
    internalSaksnummer = klage.internalSaksnummer
    fullmektig = klage.fullmektig
    language = klage.language.name
    titleKey = klage.titleKey.name
    hasVedlegg = klage.hasVedlegg
}