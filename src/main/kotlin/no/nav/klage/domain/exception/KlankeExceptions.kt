package no.nav.klage.domain.exception

class AttemptedIllegalUpdateException(override val message: String = "Illegal update attempted"): RuntimeException()
class KlankeNotFoundException(override val message: String = "Klage/anke not found"): RuntimeException()
class KlankeIsDeletedException(override val message: String = "Klage/anke is deleted"): RuntimeException()
class KlankeIsFinalizedException(override val message: String = "Klage/anke is already finalized"): RuntimeException()
class UpdateMismatchException(override val message: String = "Error in update query"): RuntimeException()
class InvalidIdentException(override val message: String = "Oppgitt identifikasjonsnummer er ugyldig."): RuntimeException()

class SectionedValidationErrorWithDetailsException(val title: String, val sections: List<ValidationSection>) :
    RuntimeException()

data class ValidationSection(val section: String, val properties: List<InvalidProperty>)

data class InvalidProperty(val field: String, val reason: String)