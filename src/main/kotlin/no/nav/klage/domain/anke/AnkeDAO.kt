package no.nav.klage.domain.anke

import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.domain.vedlegg.AnkevedleggDAO
import no.nav.klage.domain.vedlegg.Ankevedleggene
import no.nav.klage.util.getLanguageEnum
import no.nav.klage.util.toStatus
import no.nav.klage.util.toTema
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

class AnkeDAO(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AnkeDAO>(Anker)

    var foedselsnummer by Anker.foedselsnummer
    var fritekst by Anker.fritekst
    var status by Anker.status
    var modifiedByUser by Anker.modifiedByUser
    var tema by Anker.tema
    var userSaksnummer by Anker.userSaksnummer
    val vedlegg by AnkevedleggDAO referrersOn Ankevedleggene.ankeId
    var journalpostId by Anker.journalpostId
    var vedtakDate by Anker.vedtakDate
    var enhetsnummer by Anker.enhetsnummer
    var internalSaksnummer by Anker.internalSaksnummer
    var language by Anker.language
    var titleKey by Anker.titleKey
    var hasVedlegg by Anker.hasVedlegg
    var pdfDownloaded by Anker.pdfDownloaded
}

object Anker : UUIDTable("anke") {
    var foedselsnummer = text("foedselsnummer")
    var fritekst = text("fritekst").nullable()
    var status = text("status")
    var modifiedByUser = timestamp("modified_by_user").default(Instant.now())
    var tema = text("tema")
    var userSaksnummer = text("user_saksnummer").nullable()
    var vedtakDate = date("vedtak_date").nullable()
    var enhetsnummer = text("enhetsnummer").nullable()
    var internalSaksnummer = text("internal_saksnummer").nullable()
    var language = text("language").nullable()
    var titleKey = text("title_key")
    var hasVedlegg = bool("has_vedlegg").default(false)
    var pdfDownloaded = timestamp("pdf_downloaded").nullable()
    var journalpostId = varchar("journalpost_id", 50).nullable()
}

fun AnkeDAO.toAnke(): Anke {
    return Anke(
        id = id.value,
        foedselsnummer = foedselsnummer,
        fritekst = fritekst,
        status = status.toStatus(),
        modifiedByUser = modifiedByUser,
        tema = tema.toTema(),
        userSaksnummer = userSaksnummer,
        vedtakDate = vedtakDate,
        enhetsnummer = enhetsnummer,
        internalSaksnummer = internalSaksnummer,
        language = getLanguageEnum(this.language),
        titleKey = TitleEnum.valueOf(titleKey),
        hasVedlegg = hasVedlegg,
        journalpostId = journalpostId,
        vedlegg = vedlegg.map { it.toVedlegg() },
    )
}

fun AnkeDAO.fromAnke(anke: Anke) {
    foedselsnummer = anke.foedselsnummer
    fritekst = anke.fritekst
    status = anke.status.name
    modifiedByUser = Instant.now()
    tema = anke.tema.name
    userSaksnummer = anke.userSaksnummer
    vedtakDate = anke.vedtakDate
    enhetsnummer = anke.enhetsnummer
    internalSaksnummer = anke.internalSaksnummer
    language = anke.language.name
    titleKey = anke.titleKey.name
    journalpostId = anke.journalpostId
    hasVedlegg = anke.hasVedlegg
}