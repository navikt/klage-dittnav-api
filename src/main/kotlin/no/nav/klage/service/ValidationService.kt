package no.nav.klage.service

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Type
import no.nav.klage.domain.exception.*
import no.nav.klage.domain.jpa.*
import org.springframework.stereotype.Service

@Service
class ValidationService {

    fun validateKlankeAccess(klanke: Klanke, bruker: Bruker) {
        if (!klanke.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw KlankeNotFoundException()
        }
    }

    fun checkKlankeStatus(klanke: Klanke, includeFinalized: Boolean = true) {
        if (klanke.isDeleted()) {
            throw KlankeIsDeletedException()
        }

        if (includeFinalized && klanke.isFinalized()) {
            throw KlankeIsFinalizedException()
        }
    }

    fun validateKlanke(klanke: Klanke) {
        val validationErrors = mutableListOf<InvalidProperty>()

        if ((klanke.type == Type.ANKE || klanke.type == Type.KLAGE) && klanke.fritekst == null) {
            validationErrors += createMustBeFilledValidationError("fritekst")
        }
//TODO: Introduce after client sync
//
//        if (klanke.type == Type.KLAGE_ETTERSENDELSE && klanke.caseIsAtKA == null) {
//            validationErrors += createMustBeFilledValidationError("caseIstAtKa")
//        }
//
//        if (klanke.caseIsAtKA == true && klanke.enhetsnummer == null) {
//            validationErrors += createMustBeFilledValidationError("enhetsnummer")
//        }

        if ((klanke.type == Type.ANKE || klanke.type == Type.ANKE_ETTERSENDELSE) && klanke.enhetsnummer == null) {
            validationErrors += createMustBeFilledValidationError("enhetsnummer")
        }

        val sectionList = mutableListOf<ValidationSection>()

        if (validationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "klankedata",
                    properties = validationErrors
                )
            )
        }

        if (sectionList.isNotEmpty()) {
            throw SectionedValidationErrorWithDetailsException(
                title = "Validation error",
                sections = sectionList
            )
        }

    }

    private fun createMustBeFilledValidationError(variableName: String): InvalidProperty {
        return InvalidProperty(
            field = variableName,
            reason = "Må fylles ut."
        )
    }
}