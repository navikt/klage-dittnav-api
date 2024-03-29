package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
@Tag(name = "titles")
@RequestMapping("/api/titles", "/api/innsendingsytelser")
class InnsendingsytelseTitleController {

    @GetMapping
    fun getTitles(): Map<String, Innsendingsytelse.TitleInAllLanguages> {
        val output = mutableMapOf<String, Innsendingsytelse.TitleInAllLanguages>()

        Innsendingsytelse.values().forEach {
            output[it.name] = it.getTitleInAllLanguages()
        }

        return output
    }

    @GetMapping("/{language}")
    fun getTitlesForLanguage(@PathVariable language: LanguageEnum): Map<String, String> {
        val output = mutableMapOf<String, String>()

        Innsendingsytelse.values().forEach {
            output[it.name] = it.getChosenTitle(language)
        }

        return output
    }
}