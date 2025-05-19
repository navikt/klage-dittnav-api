package no.nav.klage.service

import no.nav.klage.domain.Type
import no.nav.klage.domain.exception.*
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.jpa.isAccessibleToUser
import no.nav.klage.domain.jpa.isDeleted
import no.nav.klage.domain.jpa.isFinalized
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Service

@Service
class ValidationService {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun validateKlankeAccess(klanke: Klanke, foedselsnummer: String) {
        validateKlankeAccessForIdentifikasjonsnummer(
            klanke = klanke,
            identifikasjonsnummer = foedselsnummer
        )
    }

    fun validateKlankeAccessForIdentifikasjonsnummer(klanke: Klanke, identifikasjonsnummer: String) {
        if (!klanke.isAccessibleToUser(identifikasjonsnummer)) {
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

        if (klanke.fritekst == null && klanke.vedlegg.isEmpty()) {
            validationErrors += InvalidProperty(
                field = "fritekst,vedlegg",
                reason = "Fritekst og/eller vedlegg må angis."
            )
        }

        if (klanke.type == Type.KLAGE_ETTERSENDELSE && klanke.caseIsAtKA == null) {
            validationErrors += createMustBeFilledValidationError("caseIsAtKa")
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
            logger.warn("Validation error: {}", sectionList)
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