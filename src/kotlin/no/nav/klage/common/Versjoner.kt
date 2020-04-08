package no.nav.klage.common

data class Versjon(
        val versjon: String,
        val default: Boolean,
        val beskrivelse: String
)

fun hentVersjoner(): List<Versjon> = listOf(
        Versjon(
                versjon = "v1",
                default = true,
                beskrivelse = """
                    Denne versjonen er ny
                """.trimIndent()
        )
)
