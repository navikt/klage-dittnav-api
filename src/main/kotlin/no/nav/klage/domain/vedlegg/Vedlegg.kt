package no.nav.klage.domain.vedlegg

import java.util.*

abstract class Vedlegg(
    val tittel: String,
    val ref: String,
    val contentType: String = "Ukjent",
    val id: Int? = null,
    val sizeInBytes: Int
)

class Klagevedlegg(
    val klageId: Int,
    tittel: String,
    ref: String,
    contentType: String,
    id: Int?,
    sizeInBytes: Int,
): Vedlegg(
    tittel = tittel,
    ref = ref,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes,
)

class Ankevedlegg(
    val ankeId: UUID,
    tittel: String,
    ref: String,
    contentType: String,
    id: Int?,
    sizeInBytes: Int,
): Vedlegg(
    tittel = tittel,
    ref = ref,
    contentType = contentType,
    id = id,
    sizeInBytes = sizeInBytes,
)

fun Vedlegg.toVedleggView() = VedleggView(
    tittel = tittel,
    contentType = contentType,
    id = id!!,
    sizeInBytes = sizeInBytes,
)