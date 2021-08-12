package no.nav.klage.domain.anke

import no.nav.klage.domain.ankevedlegg.AnkeVedleggDAO
import no.nav.klage.domain.ankevedlegg.AnkeVedleggene
import no.nav.klage.util.getLanguageEnum
import no.nav.klage.util.getTitleEnum
import no.nav.klage.util.toStatus
import no.nav.klage.util.toTema
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.date
import org.jetbrains.exposed.sql.`java-time`.timestamp
import java.time.Instant
import java.util.*

class AnkeDAO(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AnkeDAO>(Anker)

    var foedselsnummer by Anker.foedselsnummer
    var fritekst by Anker.fritekst
    var status by Anker.status
    var modifiedByUser by Anker.modifiedByUser
    var tema by Anker.tema
    val vedlegg by AnkeVedleggDAO referrersOn AnkeVedleggene.ankeId
    var journalpostId by Anker.journalpostId
    var vedtakDate by Anker.vedtakDate
    var internalSaksnummer by Anker.internalSaksnummer
    var fullmektig by Anker.fullmektig
    var language by Anker.language
}

object Anker : IntIdTable("anke") {
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

fun AnkeDAO.toAnke(): Anke {
    return Anke(
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

fun AnkeDAO.fromAnke(anke: Anke) {
    foedselsnummer = anke.foedselsnummer
    fritekst = anke.fritekst
    status = anke.status.name
    modifiedByUser = Instant.now()
    anke.tema.let { tema = anke.tema.name }
    anke.journalpostId?.let { journalpostId = it }
    anke.vedtakDate?.let {vedtakDate = it }
    internalSaksnummer = UUID.fromString(anke.internalSaksnummer)
    fullmektig = anke.fullmektig
    language = anke.language.name
}
