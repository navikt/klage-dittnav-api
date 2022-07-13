package no.nav.klage.controller

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IgnoreController {

    /*
    Handle request for favicon without token support throwing error.
    Maybe we shouldn't expose the BE API to the internet at all? Could be that the new rewrite of FE will use
    a proxy to reach us instead.
     */
    @Unprotected
    @GetMapping("favicon.ico")
    fun nothingToSeeHere() {
    }

    @Unprotected
    @GetMapping("ads.txt")
    fun nothingToSeeHereEither() {
    }
}
