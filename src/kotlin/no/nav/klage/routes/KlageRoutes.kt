package no.nav.klage.routes

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import no.nav.klage.common.apiCounter
import no.nav.klage.repository.KlageRepository

fun Routing.klageRoutes() {
    val klageRepository = KlageRepository()

    get("/klager") {
        apiCounter().increment()
        val klageId = call.request.queryParameters["klageid"]
        val fnr = call.request.queryParameters["fnr"]
        if (klageId != null) {
            call.respond(klageRepository.getKlagerByKlageId(klageId.toInt()))
        } else if (fnr != null) {
            call.respond(klageRepository.getKlagerByFnr(fnr))
        } else {
            call.respond(klageRepository.getKlager())
        }
    }
    get("/klager/{id}") {
        val id = call.parameters["id"]!!.toInt()
        call.respond(klageRepository.getKlageById(id))
    }
    post("/klager") {
        call.respond(HttpStatusCode.Created, klageRepository.addKlage(call.receive()))
    }
}