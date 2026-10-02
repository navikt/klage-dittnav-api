package no.nav.klage.domain

enum class KlageAnkeStatus {
    OPEN,
    DRAFT,
    DONE,
    DELETED,

    /**
     * Finalized by the user, but not yet confirmed sent to Kafka. Shown as DONE to the user.
     */
    SENDING,
}

fun KlageAnkeStatus.isDeleted() = this === KlageAnkeStatus.DELETED

fun KlageAnkeStatus.isFinalized() = this === KlageAnkeStatus.DONE || this === KlageAnkeStatus.SENDING
