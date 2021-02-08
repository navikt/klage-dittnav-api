package no.nav.klage.util

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.TitleEnum
import org.springframework.core.convert.converter.Converter

class StringToLanguageEnumConverter : Converter<String?, LanguageEnum?> {
    override fun convert(source: String?): LanguageEnum {
        if (source != null) {
            return LanguageEnum.valueOf(source.toUpperCase())
        } else {
            throw RuntimeException("error")
        }
    }
}

class StringToTitleEnumConverter : Converter<String?, TitleEnum?> {
    override fun convert(source: String?): TitleEnum {
        if (source != null) {
            return TitleEnum.valueOf(source.toUpperCase())
        } else {
            throw RuntimeException("error")
        }
    }
}