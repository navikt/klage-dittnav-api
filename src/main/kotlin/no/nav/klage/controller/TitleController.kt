package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.TitleEnum
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
@Tag(name = "titles")
@RequestMapping("/titles")
class TitleController {

    @GetMapping
    fun getTitles(): MutableMap<String, TitleEnum.TitleInAllLanguages> {
        val output = mutableMapOf<String, TitleEnum.TitleInAllLanguages>()

        TitleEnum.values().forEach {
            output[it.name] = it.getTitleInAllLanguages()
        }

        return output
    }

    @GetMapping("/{language}")
    fun getTitlesFromLanguage(@PathVariable language: LanguageEnum): MutableMap<String, String> {
        val output = mutableMapOf<String, String>()

        TitleEnum.values().forEach {
            output[it.name] = it.getChosenTitle(language)
        }

        return output
    }
}