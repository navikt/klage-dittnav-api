package no.nav.klage.domain.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Sak(
    @Column(name = "sak_type")
    var sakstype: String?,
    @Column(name = "sak_fagsaksystem")
    var fagsaksystem: String?,
    @Column(name = "sak_fagsakid")
    var fagsakid: String?,
)