package no.nav.klage.service

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.anke.Anke
import no.nav.klage.domain.anke.isAccessibleToUser
import no.nav.klage.domain.anke.isDeleted
import no.nav.klage.domain.anke.isFinalized
import no.nav.klage.domain.ankeOLD.AnkeOLD
import no.nav.klage.domain.ankeOLD.isAccessibleToUser
import no.nav.klage.domain.ankeOLD.isDeleted
import no.nav.klage.domain.ankeOLD.isFinalized
import no.nav.klage.domain.exception.*
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.klage.isAccessibleToUser
import no.nav.klage.domain.klage.isDeleted
import no.nav.klage.domain.klage.isFinalized
import org.springframework.stereotype.Service

@Service
class ValidationService(
    private val brukerService: BrukerService
) {

    fun validateKlageAccess(klage: Klage, bruker: Bruker) {
        if (klage.fullmektig != null && klage.fullmektig == bruker.folkeregisteridentifikator.identifikasjonsnummer) {
            val fullmaktsGiver = brukerService.getFullmaktsgiver(klage.tema, klage.foedselsnummer)
            if (!klage.isAccessibleToUser(fullmaktsGiver.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw KlageNotFoundException()
            }
        } else {
            if (!klage.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw KlageNotFoundException()
            }
        }
    }

    fun validateAnkeAccessOLD(ankeOLD: AnkeOLD, bruker: Bruker) {
        if (ankeOLD.fullmektig != null && ankeOLD.fullmektig == bruker.folkeregisteridentifikator.identifikasjonsnummer) {
            val fullmaktsGiver = brukerService.getFullmaktsgiver(ankeOLD.tema, ankeOLD.foedselsnummer)
            if (!ankeOLD.isAccessibleToUser(fullmaktsGiver.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw AnkeNotFoundException()
            }
        } else {
            if (!ankeOLD.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw AnkeNotFoundException()
            }
        }
    }

    fun validateAnkeAccess(anke: Anke, bruker: Bruker) {
        if (!anke.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
            throw AnkeNotFoundException()
        }
    }

    fun checkKlageStatus(klage: Klage, includeFinalized: Boolean = true) {
        if (klage.isDeleted()) {
            throw KlageIsDeletedException()
        }

        if (includeFinalized && klage.isFinalized()) {
            throw KlageIsFinalizedException()
        }
    }

    fun checkAnkeStatusOLD(ankeOLD: AnkeOLD, includeFinalized: Boolean = true) {
        if (ankeOLD.isDeleted()) {
            throw AnkeIsDeletedException()
        }

        if (includeFinalized && ankeOLD.isFinalized()) {
            throw AnkeIsFinalizedException()
        }
    }

    fun checkAnkeStatus(anke: Anke, includeFinalized: Boolean = true) {
        if (anke.isDeleted()) {
            throw AnkeIsDeletedException()
        }

        if (includeFinalized && anke.isFinalized()) {
            throw AnkeIsFinalizedException()
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