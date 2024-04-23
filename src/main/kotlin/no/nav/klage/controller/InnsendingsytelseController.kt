package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import no.nav.klage.kodeverk.innsendingsytelse.innsendingsytelseToDisplayName
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
@Tag(name = "titles")
@RequestMapping("/api/innsendingsytelser")
class InnsendingsytelseTitleController {

    @GetMapping("/{language}")
    fun getTitlesForLanguage(@PathVariable language: LanguageEnum): Map<String, String> {
        val output = mutableMapOf<String, String>()

        Innsendingsytelse.entries.forEach {
            when (language) {
                LanguageEnum.NB -> output[it.name] = innsendingsytelseToDisplayName[it]!!.nb
                LanguageEnum.EN -> output[it.name] = innsendingsytelseToDisplayName[it]!!.en
                LanguageEnum.NN -> output[it.name] = innsendingsytelseToDisplayName[it]!!.nn
            }
        }

        return output
    }
}