package no.nav.klage.service

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.anke.Anke
import no.nav.klage.domain.anke.isAccessibleToUser
import no.nav.klage.domain.anke.isDeleted
import no.nav.klage.domain.anke.isFinalized
import no.nav.klage.domain.exception.*
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.klage.isDeleted
import no.nav.klage.domain.klage.isFinalized
import no.nav.klage.domain.klage.isAccessibleToUser
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

    fun validateAnkeAccess(anke: Anke, bruker: Bruker) {
        if (anke.fullmektig != null && anke.fullmektig == bruker.folkeregisteridentifikator.identifikasjonsnummer) {
            val fullmaktsGiver = brukerService.getFullmaktsgiver(anke.tema, anke.foedselsnummer)
            if (!anke.isAccessibleToUser(fullmaktsGiver.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw AnkeNotFoundException()
            }
        } else {
            if (!anke.isAccessibleToUser(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw AnkeNotFoundException()
            }
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

    fun checkAnkeStatus(anke: Anke, includeFinalized: Boolean = true) {
        if (anke.isDeleted()) {
            throw AnkeIsDeletedException()
        }

        if (includeFinalized && anke.isFinalized()) {
            throw AnkeIsFinalizedException()
        }
    }
}