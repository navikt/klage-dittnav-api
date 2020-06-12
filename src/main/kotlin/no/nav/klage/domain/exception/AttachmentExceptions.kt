package no.nav.klage.domain.exception

class AttachmentTooLargeException(override val message: String = "TOO_LARGE") : RuntimeException()
class AttachmentTotalTooLargeException(override val message: String = "TOTAL_TOO_LARGE") : RuntimeException()
class AttachmentEncryptedException(override val message: String = "ENCRYPTED") : RuntimeException()
class AttachmentIsEmptyException(override val message: String = "EMPTY") : RuntimeException()
class AttachmentHasVirusException(override val message: String = "VIRUS") : RuntimeException()