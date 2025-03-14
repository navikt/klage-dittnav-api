package no.nav.klage.domain.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
data class Sak(
    @Column(name = "sak_type")
    @Enumerated(EnumType.STRING)
    var sakstype: Sakstype?,
    @Column(name = "sak_fagsaksystem")
    var fagsaksystem: String?,
    @Column(name = "sak_fagsakid")
    var fagsakid: String?,
)

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK,
    ARKIVSAK
}