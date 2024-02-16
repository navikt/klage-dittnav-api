package no.nav.klage.controller.view

import java.util.UUID

data class VedleggView(
    val tittel: String,
    val contentType: String = "Ukjent",
    val id: UUID,
    val sizeInBytes: Int,
)