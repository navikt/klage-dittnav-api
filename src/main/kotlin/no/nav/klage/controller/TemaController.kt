package no.nav.klage.controller

import no.nav.klage.domain.Tema
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
class TemaController {

    @GetMapping("/temaer")
    fun getTemaer(): Array<Tema> {
        return Tema.values()
    }
}

