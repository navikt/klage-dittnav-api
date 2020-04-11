package no.nav.klage.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import no.nav.klage.repository.KlageRepository

fun Routing.klageRoutes() {
    val klageRepository = KlageRepository()

    get("/klager") {
        call.respond(klageRepository.getKlager())
    }
    post("/klager") {
        call.respond(HttpStatusCode.Created, klageRepository.addKlage(call.receive()))
    }
}