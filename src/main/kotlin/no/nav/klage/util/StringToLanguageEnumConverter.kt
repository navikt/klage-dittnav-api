package no.nav.klage.util

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import org.springframework.core.convert.converter.Converter
import java.util.Locale


class StringToLanguageEnumConverter : Converter<String, LanguageEnum?> {
    override fun convert(source: String): LanguageEnum {
        return LanguageEnum.valueOf(source.uppercase(Locale.getDefault()))
    }
}

class StringToTitleEnumConverter : Converter<String, Innsendingsytelse?> {
    override fun convert(source: String): Innsendingsytelse {
        return Innsendingsytelse.valueOf(source.uppercase(Locale.getDefault()))
    }
}