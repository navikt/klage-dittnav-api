package no.nav.klage.domain.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Type
import no.nav.klage.domain.isDeleted
import no.nav.klage.domain.isFinalized
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.InnsendingsytelseConverter
import no.nav.klage.util.klageAnkeIsAccessibleToUser
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "klanke")
@DynamicUpdate
class Klanke(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "foedselsnummer")
    var foedselsnummer: String,
    @Column(name = "fritekst")
    var fritekst: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: KlageAnkeStatus,
    @Column(name = "user_saksnummer")
    var userSaksnummer: String?,
    @Column(name = "journalpost_id")
    var journalpostId: String?,
    @Column(name = "vedtak_date")
    var vedtakDate: LocalDate?,
    @Embedded
    var sak: Sak?,
    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    var language: LanguageEnum,
    @Column(name = "innsendingsytelse_id")
    @Convert(converter = InnsendingsytelseConverter::class)
    var innsendingsytelse: Innsendingsytelse,
    @Column(name = "has_vedlegg")
    var hasVedlegg: Boolean,
    @Column(name = "pdf_downloaded")
    var pdfDownloaded: LocalDateTime?,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "klanke_id", referencedColumnName = "id", nullable = false)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 5)
    val vedlegg: MutableSet<Vedlegg> = mutableSetOf(),
    @Column(name = "created")
    var created: LocalDateTime,
    @Column(name = "modified_by_user")
    var modifiedByUser: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(name = "klanke_type")
    var type: Type,
    // ettersendelser klage
    @Column(name = "case_is_at_ka")
    var caseIsAtKA: Boolean?,
    @Column(name = "fullmektig_foedselsnummer")
    var fullmektigFoedselsnummer: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Klanke

        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "Klanke(id=$id, type=$type)"
}

fun Klanke.isAccessibleToUser(usersIdentifikasjonsnummer: String) =
    klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer = usersIdentifikasjonsnummer, klageAnkeIdentifikasjonsnummer = foedselsnummer)

fun Klanke.isFinalized() = status.isFinalized()

fun Klanke.isDeleted() = status.isDeleted()
