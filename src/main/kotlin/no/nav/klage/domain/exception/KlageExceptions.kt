package no.nav.klage.domain.exception

class AttemptedIllegalUpdateException(override val message: String = "Illegal update attempted"): RuntimeException()
class KlageNotFoundException(override val message: String = "Klage not found"): RuntimeException()
class KlageIsDeletedException(override val message: String = "Klage is deleted"): RuntimeException()
class KlageIsFinalizedException(override val message: String = "Klage is already finalized"): RuntimeException()
class UpdateMismatchException(override val message: String = "Error in update query"): RuntimeException()

class AnkeNotFoundException(override val message: String = "Anke not found"): RuntimeException()
class AnkeIsDeletedException(override val message: String = "Anke is deleted"): RuntimeException()
class AnkeIsFinalizedException(override val message: String = "Anke is already finalized"): RuntimeException()