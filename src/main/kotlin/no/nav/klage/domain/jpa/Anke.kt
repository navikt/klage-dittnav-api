package no.nav.klage.domain.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.Innsendingsytelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@DiscriminatorValue("anke")
class Anke(
    @Column(name = "enhetsnummer")
    var enhetsnummer: String?,

    //common
    id: UUID = UUID.randomUUID(),
    foedselsnummer: String,
    fritekst: String?,
    status: KlageAnkeStatus,
    tema: Tema,
    userSaksnummer: String?,
    journalpostId: String?,
    vedtakDate: LocalDate?,
    internalSaksnummer: String?,
    language: LanguageEnum,
    innsendingsytelse: Innsendingsytelse,
    hasVedlegg: Boolean,
    pdfDownloaded: LocalDateTime?,
    vedlegg: MutableSet<Vedlegg> = mutableSetOf(),
    created: LocalDateTime,
    modifiedByUser: LocalDateTime,
) : Klanke(
    id = id,
    foedselsnummer = foedselsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    userSaksnummer = userSaksnummer,
    journalpostId = journalpostId,
    vedtakDate = vedtakDate,
    internalSaksnummer = internalSaksnummer,
    language = language,
    innsendingsytelse = innsendingsytelse,
    hasVedlegg = hasVedlegg,
    pdfDownloaded = pdfDownloaded,
    vedlegg = vedlegg,
    created = created,
    modifiedByUser = modifiedByUser,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Anke

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Anke(id=$id)"
    }
}