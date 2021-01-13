package no.nav.klage.service

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.klage.isDeleted
import no.nav.klage.domain.klage.isFinalized
import no.nav.klage.domain.klage.validateAccess
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ValidationService(
    private val brukerService: BrukerService
) {

    fun validateAccess(klage: Klage, bruker: Bruker) {
        if (klage.fullmektig != null && klage.fullmektig == bruker.folkeregisteridentifikator.identifikasjonsnummer) {
            val fullmaktsGiver = brukerService.getFullmaktsgiver(klage.tema, klage.foedselsnummer)
            if (!klage.validateAccess(fullmaktsGiver.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
            }
        } else {
            if (!klage.validateAccess(bruker.folkeregisteridentifikator.identifikasjonsnummer)) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Klage not found")
            }
        }
    }

    fun checkKlageStatus(klage: Klage, includeFinalized: Boolean = true) {
        if (klage.isDeleted()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Klage is deleted.")
        }

        if (includeFinalized && klage.isFinalized()) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Klage is already finalized.")
        }
    }
}