package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.KlagevedleggDAO
import no.nav.klage.domain.vedlegg.Klagevedleggene
import no.nav.klage.util.getLanguageEnum
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
    var userSaksnummer by Klager.userSaksnummer
    val vedlegg by KlagevedleggDAO referrersOn Klagevedleggene.klageId
    var journalpostId by Klager.journalpostId
    var vedtakDate by Klager.vedtakDate
    var checkBoxesSelected by Klager.checkboxesSelected
    var internalSaksnummer by Klager.internalSaksnummer
    var fullmektig by Klager.fullmektig
    var language by Klager.language
    var innsendingsytelse by Klager.innsendingsytelse
    var hasVedlegg by Klager.hasVedlegg
    var pdfDownloaded by Klager.pdfDownloaded
}

object Klager : IntIdTable("klage") {
    var foedselsnummer = varchar("foedselsnummer", 11)
    var fritekst = text("fritekst").nullable()
    var status = varchar("status", 15)
    var modifiedByUser = timestamp("modified_by_user").default(Instant.now())
    var tema = varchar("tema", 3)
    var userSaksnummer = text("user_saksnummer").nullable()
    var journalpostId = varchar("journalpost_id", 50).nullable()
    var vedtakDate = date("vedtak_date").nullable()
    var checkboxesSelected = text("checkboxes_selected").nullable()
    var internalSaksnummer = text("internal_saksnummer").nullable()
    var fullmektig = varchar("fullmektig", 11).nullable()
    var language = text("language").nullable()
    var innsendingsytelse = text("innsendingsytelse")
    var hasVedlegg = bool("has_vedlegg").default(false)
    var pdfDownloaded = timestamp("pdf_downloaded").nullable()
}

fun KlageDAO.toKlage(): Klage {
    return Klage(
        id = id.toString(),
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
        innsendingsytelse = Innsendingsytelse.valueOf(innsendingsytelse),
        hasVedlegg = hasVedlegg,
    )
}

fun String.toCheckboxEnumSet() =
    if (this == "") {
        emptySet()
    } else {
        this.split(",").map { x -> CheckboxEnum.valueOf(x) }.toSet()
    }

fun KlageDAO.fromKlageFullInput(klageFullInput: KlageFullInput, bruker: Bruker) {
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer
    fritekst = klageFullInput.fritekst
    status = KlageAnkeStatus.DRAFT.name
    modifiedByUser = Instant.now()
    tema = klageFullInput.innsendingsytelse.toTema().name
    userSaksnummer = klageFullInput.userSaksnummer
    journalpostId = null
    vedtakDate = klageFullInput.vedtakDate
    klageFullInput.checkboxesSelected.let { checkBoxesSelected = it.joinToString(",") { x -> x.toString() } }
    internalSaksnummer = klageFullInput.internalSaksnummer
    fullmektig = null
    language = klageFullInput.language.name
    innsendingsytelse = klageFullInput.innsendingsytelse.name
    hasVedlegg = klageFullInput.hasVedlegg
}

fun KlageDAO.fromKlageInput(klageInput: KlageInput, bruker: Bruker) {
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer
    fritekst = null
    status = KlageAnkeStatus.DRAFT.name
    modifiedByUser = Instant.now()
    tema = klageInput.innsendingsytelse.toTema().name
    userSaksnummer = null
    journalpostId = null
    vedtakDate = null
    checkBoxesSelected = null
    internalSaksnummer = klageInput.internalSaksnummer
    fullmektig = null
    language = LanguageEnum.NB.name
    innsendingsytelse = klageInput.innsendingsytelse.name
    hasVedlegg = false
}