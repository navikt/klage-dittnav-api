package no.nav.klage.domain.ankeOLD

import no.nav.klage.domain.ankevedleggOLD.AnkeVedleggOLDDAO
import no.nav.klage.domain.ankevedleggOLD.AnkeVedleggene
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
import java.util.*

class AnkeOLDDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AnkeOLDDAO>(Anker)

    var foedselsnummer by Anker.foedselsnummer
    var fritekst by Anker.fritekst
    var status by Anker.status
    var modifiedByUser by Anker.modifiedByUser
    var tema by Anker.tema
    val vedlegg by AnkeVedleggOLDDAO referrersOn AnkeVedleggene.ankeId
    var journalpostId by Anker.journalpostId
    var vedtakDate by Anker.vedtakDate
    var internalSaksnummer by Anker.internalSaksnummer
    var fullmektig by Anker.fullmektig
    var language by Anker.language
}

object Anker : IntIdTable("anke_old") {
    var foedselsnummer = varchar("foedselsnummer", 11)
    var fritekst = text("fritekst")
    var status = varchar("status", 15)
    var modifiedByUser = timestamp("modified_by_user").default(Instant.now())
    var tema = varchar("tema", 3)
    var journalpostId = varchar("journalpost_id", 50).nullable()
    var vedtakDate = date("vedtak_date").nullable()
    var internalSaksnummer = uuid("internal_saksnummer")
    var fullmektig = varchar("fullmektig", 11).nullable()
    var language = text("language").nullable()
}

fun AnkeOLDDAO.toAnke(): AnkeOLD {
    return AnkeOLD(
        id = id.toString().toInt(),
        foedselsnummer = foedselsnummer,
        fritekst = fritekst,
        status = status.toStatus(),
        modifiedByUser = modifiedByUser,
        tema = tema.toTema(),
        vedlegg = vedlegg.map { it.toAnkeVedlegg() },
        journalpostId = journalpostId,
        vedtakDate = vedtakDate,
        internalSaksnummer = internalSaksnummer.toString(),
        fullmektig = fullmektig,
        language = getLanguageEnum(this.language)
    )
}

fun AnkeOLDDAO.fromAnke(ankeOLD: AnkeOLD) {
    foedselsnummer = ankeOLD.foedselsnummer
    fritekst = ankeOLD.fritekst
    status = ankeOLD.status.name
    modifiedByUser = Instant.now()
    ankeOLD.tema.let { tema = ankeOLD.tema.name }
    ankeOLD.journalpostId?.let { journalpostId = it }
    ankeOLD.vedtakDate?.let {vedtakDate = it }
    internalSaksnummer = UUID.fromString(ankeOLD.internalSaksnummer)
    fullmektig = ankeOLD.fullmektig
    language = ankeOLD.language.name
}
