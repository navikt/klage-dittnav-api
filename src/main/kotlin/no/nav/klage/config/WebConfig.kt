package no.nav.klage.config

import no.nav.klage.util.StringToLanguageEnumConverter
import no.nav.klage.util.StringToTitleEnumConverter
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer


@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToLanguageEnumConverter())
        registry.addConverter(StringToTitleEnumConverter())
    }
}