package no.nav.klage.domain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.codec.ServerSentEvent

data class Event (
    val klageId: String,
    val name: String,
    val id: String,
    val data: String,
)

fun jsonToEvent(json: String?): Event {
    val event = jacksonObjectMapper().readValue(json, Event::class.java)
    return event
}

fun Event.toServerSentEvent(): ServerSentEvent<JsonNode> {
    return ServerSentEvent.builder<JsonNode>()
        .id(klageId)
        .event(name)
        .data(jacksonObjectMapper().readTree(data))
        .build()
}

fun Long.toHeartBeatServerSentEvent(): ServerSentEvent<JsonNode> {
    return Event(
        klageId = "",
        name = "heartbeat-event-$this",
        id = "",
        data = "{}"
    ).toServerSentEvent()
}