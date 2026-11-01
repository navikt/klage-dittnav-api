package no.nav.klage.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IgnoreController {
    /*
    Handle request for favicon without an error.
    Maybe we shouldn't expose the BE API to the internet at all? Could be that the new rewrite of FE will use
    a proxy to reach us instead.
     */
    @GetMapping("favicon.ico")
    fun nothingToSeeHere() {
    }

    @GetMapping("ads.txt")
    fun nothingToSeeHereEither() {
    }

    @GetMapping("robots.txt")
    fun stillNothingToSeeHere() {
    }
}
