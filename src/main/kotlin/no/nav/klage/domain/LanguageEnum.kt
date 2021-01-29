package no.nav.klage.domain

import com.fasterxml.jackson.annotation.JsonProperty

enum class LanguageEnum {
    @JsonProperty("no")
    NO,
    @JsonProperty("nb")
    NB,
    @JsonProperty("en")
    EN
}

