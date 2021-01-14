package no.nav.klage.domain.exception

class AttemptedIllegalUpdateException(override val message: String = "Illegal update attempted"): RuntimeException()
class KlageNotFoundException(override val message: String = "Klage not found"): RuntimeException()
class KlageIsDeletedException(override val message: String = "Klage is deleted"): RuntimeException()
class KlageIsFinalizedException(override val message: String = "Klage is already finalized"): RuntimeException()