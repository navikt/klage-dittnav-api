package no.nav.klage.common

import io.ktor.application.ApplicationCall
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.request.uri
import io.ktor.response.respond
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

fun StatusPages.Configuration.exceptionHandler() {
//    exception<GraphqlError> { cause ->
//        call.logErrorAndRespond(cause, HttpStatusCode.InternalServerError) {
//            "Feil fra graphql i $cause.system: ${cause.errorAsJson()}"
//        }
//    }
//
//    exception<IdenterIkkeFunnet> { cause ->
//        call.logErrorAndRespond(cause, HttpStatusCode.NotFound) {
//            "Fant ingen aktør-id for fødselsnummer"
//        }
//    }
//
//    exception<PersonIkkeFunnet> { cause ->
//        call.logErrorAndRespond(cause, HttpStatusCode.NotFound) {
//            "Person ikke funnet i ${cause.system}"
//        }
//    }
//
//    exception<Sikkerhetsbegrensing> { cause ->
//        call.logErrorAndRespond(cause, HttpStatusCode.Forbidden) {
//            "Personen har sikkerhetsbegrensing i ${cause.system}"
//        }
//    }
//
//    exception<ClientRequestException> { cause ->
//        call.logErrorAndRespond(cause, HttpStatusCode.InternalServerError) {
//            val url = cause.response.call.request.url
//            "Kall mot $url feilet"
//        }
//
//    }
//
//    exception<ServerResponseException> { cause ->
//        call.logErrorAndRespond(cause, HttpStatusCode.InternalServerError) {
//            val url = cause.response.call.request.url
//            "Kall mot $url feilet"
//        }
//    }
//
//    exception<BadRequestException> { cause ->
//        call.logErrorAndRespond(cause, HttpStatusCode.BadRequest) {
//            cause.message!!
//        }
//    }
//
//    exception<Throwable> { cause ->
//        call.logErrorAndRespond(cause) {
//            "An internal error occurred during routing"
//        }
//    }
}

private suspend inline fun ApplicationCall.logErrorAndRespond(
        cause: Throwable,
        status: HttpStatusCode = HttpStatusCode.InternalServerError,
        lazyMessage: () -> String
) {
    val message = lazyMessage()
    logger.error(cause) { message }
    val response = HttpErrorResponse(
            url = this.request.uri,
            cause = cause.toString(),
            message = message,
            code = status,
            callId = getCorrelationId()
    )
    this.respond(status, response)
}

internal data class HttpErrorResponse(
        val url: String,
        val message: String? = null,
        val cause: String? = null,
        val code: HttpStatusCode = HttpStatusCode.InternalServerError,
        val callId: CorrelationId? = null
)
