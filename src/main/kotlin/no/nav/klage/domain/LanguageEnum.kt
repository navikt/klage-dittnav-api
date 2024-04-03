package no.nav.klage.domain

import com.fasterxml.jackson.annotation.JsonProperty

enum class LanguageEnum {
    @JsonProperty("nb")
    NB,
    @JsonProperty("en")
    EN,
    @JsonProperty("nn")
    NN,
}

