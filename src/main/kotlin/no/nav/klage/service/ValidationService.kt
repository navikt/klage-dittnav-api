package no.nav.klage.service

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.exception.*
import no.nav.klage.domain.jpa.*
import org.springframework.stereotype.Service

@Service
class ValidationService {

    fun validateKlageAccess(klage: Klage, bruker: Bruker) {
        if (!klage.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw KlageNotFoundException()
        }
    }

    fun validateAnkeAccess(anke: Anke, bruker: Bruker) {
        if (!anke.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw AnkeNotFoundException()
        }
    }

    fun validateKlankeAccess(klanke: Klanke, bruker: Bruker) {
        if (!klanke.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            when (klanke) {
                is Klage -> throw KlageNotFoundException()
                is Anke -> throw AnkeNotFoundException()
            }
        }
    }

    fun checkKlankeStatus(klanke: Klanke, includeFinalized: Boolean = true) {
        if (klanke.isDeleted()) {
            when (klanke) {
                is Klage -> throw KlageIsDeletedException()
                is Anke -> throw AnkeIsDeletedException()
            }
        }

        if (includeFinalized && klanke.isFinalized()) {
            when (klanke) {
                is Klage -> throw KlageIsFinalizedException()
                is Anke -> throw AnkeIsFinalizedException()
            }

        }
    }

    fun validateKlage(klage: Klage) {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (klage.fritekst == null) {
            validationErrors += createMustBeFilledValidationError("fritekst")
        }

        val sectionList = mutableListOf<ValidationSection>()

        if (validationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "klagedata",
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

    fun validateAnke(anke: Anke) {
        val validationErrors = mutableListOf<InvalidProperty>()

        if (anke.enhetsnummer == null) {
            validationErrors += createMustBeFilledValidationError("enhetsnummer")
        }

        if (anke.fritekst == null) {
            validationErrors += createMustBeFilledValidationError("fritekst")
        }

        val sectionList = mutableListOf<ValidationSection>()

        if (validationErrors.isNotEmpty()) {
            sectionList.add(
                ValidationSection(
                    section = "ankedata",
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