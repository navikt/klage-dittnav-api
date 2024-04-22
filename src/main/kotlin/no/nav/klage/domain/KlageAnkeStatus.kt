package no.nav.klage.domain

enum class KlageAnkeStatus {
    OPEN, DRAFT, DONE, DELETED
}

fun KlageAnkeStatus.isDeleted() = this === KlageAnkeStatus.DELETED
fun KlageAnkeStatus.isFinalized() = this === KlageAnkeStatus.DONE
