package no.nav.klage.domain.jpa

import jakarta.persistence.*
import no.nav.klage.domain.*
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.util.klageAnkeIsAccessibleToUser
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "klanke")
@DynamicUpdate
@DiscriminatorColumn(name = "klanke_type")
abstract class Klanke(
    @Id
    open val id: UUID = UUID.randomUUID(),
    @Column(name = "foedselsnummer")
    open var foedselsnummer: String,
    @Column(name = "fritekst")
    open var fritekst: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    open var status: KlageAnkeStatus,
    @Enumerated(EnumType.STRING)
    @Column(name = "tema")
    open var tema: Tema,
    @Column(name = "user_saksnummer")
    open var userSaksnummer: String?,
    @Column(name = "journalpost_id")
    open var journalpostId: String?,
    @Column(name = "vedtak_date")
    open var vedtakDate: LocalDate?,
    @Column(name = "internal_saksnummer")
    open var internalSaksnummer: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "language")
    open var language: LanguageEnum,
    @Enumerated(EnumType.STRING)
    @Column(name = "innsendingsytelse")
    open var innsendingsytelse: Innsendingsytelse,
    @Column(name = "has_vedlegg")
    open var hasVedlegg: Boolean,
    @Column(name = "pdf_downloaded")
    open var pdfDownloaded: LocalDateTime?,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "klanke_id", referencedColumnName = "id", nullable = false)
    @Fetch(FetchMode.SELECT)
    @BatchSize(size = 5)
    open val vedlegg: MutableSet<Vedlegg> = mutableSetOf(),

    @Column(name = "created")
    open var created: LocalDateTime,
    @Column(name = "modified_by_user")
    open var modifiedByUser: LocalDateTime,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Klanke

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Klanke(id=$id)"
    }
}

fun Klanke.isAccessibleToUser(usersIdentifikasjonsnummer: String) = klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)
fun Klanke.isFinalized() = status.isFinalized()
fun Klanke.isDeleted() = status.isDeleted()