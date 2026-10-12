package no.nav.klage.domain

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.codec.ServerSentEvent
import java.util.UUID

const val JOURNALPOST_ID_EVENT_NAME = "journalpostId"

data class Event(
    val klageAnkeId: String,
    val name: String,
    val id: String,
    val data: String,
)

fun journalpostIdEvent(
    klankeId: UUID,
    journalpostId: String,
): Event =
    Event(
        klageAnkeId = klankeId.toString(),
        name = JOURNALPOST_ID_EVENT_NAME,
        id = klankeId.toString(),
        data = journalpostId,
    )

fun jsonToEvent(json: String?): Event = jacksonObjectMapper().readValue(json, Event::class.java)

fun Event.toServerSentEvent(): ServerSentEvent<String> =
    ServerSentEvent
        .builder<String>()
        .id(id)
        .event(name)
        .data(data)
        .build()

fun Long.toHeartBeatServerSentEvent(): ServerSentEvent<String> =
    Event(
        klageAnkeId = "",
        name = "heartbeat-event",
        id = this.toString(),
        data = "",
    ).toServerSentEvent()
