package no.nav.klage.controller

import no.nav.klage.domain.*
import no.nav.klage.service.KlageService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@Unprotected
class TriggerTestKlageController(private val klageService: KlageService) {

    @GetMapping("triggerKlage")
    fun triggerKlage(): String {
        val klage = Klage(
            foedselsnummer = "10108000398",
            fritekst = "Fritekst",
            tema = Tema.SYK,
            status = KlageStatus.DONE,
            vedtaksdato = LocalDate.now(),
            enhetId = "123",
            referanse = "En referanse"
        )

        val bruker = Bruker(
            Navn(fornavn = "Kalle", mellomnavn = null, etternavn = "Anka"),
            Adresse(adressenavn = "Veien", poststed = "Oslo", postnummer = "1234", husnummer = "6", husbokstav = "B"),
            Kontaktinformasjon(telefonnummer = "12345678", epost = "fake@fake.no"),
            Identifikator("FNR", "10108000398")
        )

        val startedKlage = klageService.createKlage(klage, bruker)
        klageService.finalizeKlage(startedKlage.id!!, bruker)
        return "Test klage created and finalized"
    }

}