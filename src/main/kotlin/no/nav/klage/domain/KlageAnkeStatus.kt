package no.nav.klage.domain

enum class KlageAnkeStatus {
    DRAFT, DONE, DELETED
}

fun KlageAnkeStatus.isDeleted() = this === KlageAnkeStatus.DELETED
fun KlageAnkeStatus.isFinalized() = this === KlageAnkeStatus.DONE