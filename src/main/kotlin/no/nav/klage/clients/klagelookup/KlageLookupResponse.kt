package no.nav.klage.clients.klagelookup

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.klage.kodeverk.Tema
import java.io.Serializable
import java.time.LocalDate

data class RepresentasjonsforholdView(
    val fullmakt: List<FullmaktsforholdView>,
    val vergemaal: List<VergemaalsforholdView>,
) : Serializable

data class FullmaktsforholdView(
    val fullmaktsgiver: String,
    val fullmektig: String,
    val leserettigheter: Set<Tema>,
    val skriverettigheter: Set<Tema>,
) : Serializable

data class VergemaalsforholdView(
    val vergehaver: String,
    val verge: String,
    val leserettigheter: Set<Tema>,
    val skriverettigheter: Set<Tema>,
) : Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonResponse(
    val foedselsnr: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val sammensattNavn: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PersonWithAllInfoResponse(
    val foedselsnr: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val sammensattNavn: String,
    val kjoenn: String?,
    val doed: LocalDate?,
    val strengtFortrolig: Boolean,
    val strengtFortroligUtland: Boolean,
    val fortrolig: Boolean,
    val egenAnsatt: Boolean,
    val vergemaalEllerFremtidsfullmakt: Boolean,
    val sikkerhetstiltak: Sikkerhetstiltak?,
) {
    data class Sikkerhetstiltak(
        val tiltakstype: Tiltakstype,
        val beskrivelse: String,
        val gyldigFraOgMed: LocalDate,
        val gyldigTilOgMed: LocalDate,
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 1L
        }

        enum class Tiltakstype {
            FYUS,
            TFUS,
            FTUS,
            DIUS,
            TOAN,
        }
    }
}