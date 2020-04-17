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
        call.respond(klageRepository.getKlager())
    }
    get("/klager/{id}") {
        val id = call.parameters["id"]!!.toInt()
        call.respond(klageRepository.getKlageById(id))
    }
    get("/klager/klageid/{id}") {
        val id = call.parameters["id"]!!.toInt()
        call.respond(klageRepository.getKlagerByKlageId(id))
    }
    get("/klager/fnr/{fnr}") {
        val fnr = call.parameters["fnr"]!!
        call.respond(klageRepository.getKlagerByFnr(fnr))
    }
    post("/klager") {
        call.respond(HttpStatusCode.Created, klageRepository.addKlage(call.receive()))
    }
}