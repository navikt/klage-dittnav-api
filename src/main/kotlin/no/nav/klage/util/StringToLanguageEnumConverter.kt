package no.nav.klage.util

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import org.springframework.core.convert.converter.Converter
import java.util.*

class StringToLanguageEnumConverter : Converter<String?, LanguageEnum?> {
    override fun convert(source: String): LanguageEnum {
        if (source != null) {
            return LanguageEnum.valueOf(source.uppercase(Locale.getDefault()))
        } else {
            throw RuntimeException("error")
        }
    }
}

class StringToTitleEnumConverter : Converter<String?, Innsendingsytelse?> {
    override fun convert(source: String): Innsendingsytelse {
        if (source != null) {
            return Innsendingsytelse.valueOf(source.uppercase(Locale.getDefault()))
        } else {
            throw RuntimeException("error")
        }
    }
}