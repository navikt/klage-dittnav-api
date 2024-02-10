package no.nav.klage.domain.jpa

import jakarta.persistence.*
import org.hibernate.Hibernate
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "vedlegg")
class Vedlegg(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tittel")
    var tittel: String,
    @Column(name = "ref")
    var ref: String,
    @Column(name = "content_type")
    var contentType: String,
    @Column(name = "size_in_bytes")
    var sizeInBytes: Int,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vedlegg

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Vedlegg(id=$id)"
    }
}