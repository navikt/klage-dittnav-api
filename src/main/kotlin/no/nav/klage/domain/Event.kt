package no.nav.klage.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.codec.ServerSentEvent

data class Event (
    val klageId: String,
    val name: String,
    val id: String,
    val data: String,
)

fun jsonToEvent(json: String?): Event =
    jacksonObjectMapper().readValue(json, Event::class.java)

fun Event.toServerSentEvent(): ServerSentEvent<String> {
    return ServerSentEvent.builder<String>()
        .id(id)
        .event(name)
        .data(data)
        .build()
}

fun Long.toHeartBeatServerSentEvent(): ServerSentEvent<String> {
    return Event(
        klageId = "",
        name = "heartbeat-event",
        id = this.toString(),
        data = ""
    ).toServerSentEvent()
}