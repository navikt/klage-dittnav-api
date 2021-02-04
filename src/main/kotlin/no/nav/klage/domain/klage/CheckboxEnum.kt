package no.nav.klage.domain.klage

import no.nav.klage.domain.LanguageEnum

//TODO: Legg inn engelsk tekst når disse fins
enum class CheckboxEnum {
    AVSLAG_PAA_SOKNAD {
        override fun getFullText(language: LanguageEnum): String {
            return "Jeg har fått avslag på søknaden min"

//            return when(language) {
//                LanguageEnum.NB -> "Jeg har fått avslag på søknaden min"
//                LanguageEnum.EN -> "TEMPLATE ENGLISH AVSLAG_PAA_SOKNAD"
//            }
        }
    },
    FOR_LITE_UTBETALT {
        override fun getFullText(language: LanguageEnum): String {
            return "Jeg har fått for lite utbetalt"

//            return when(language) {
//                LanguageEnum.NB -> "Jeg har fått for lite utbetalt"
//                LanguageEnum.EN -> "TEMPLATE ENGLISH FOR_LITE_UTBETALT"
//            }
        }
    },

    UENIG_I_VEDTAK_OM_TILBAKEBETALING {
        override fun getFullText(language: LanguageEnum): String {
            return "Jeg er uenig i vedtaket om tilbakebetaling"

//            return when(language) {
//                LanguageEnum.NB -> "Jeg er uenig i vedtaket om tilbakebetaling"
//                LanguageEnum.EN -> "TEMPLATE ENGLISH UENIG_I_VEDTAK_OM_TILBAKEBETALING"
//            }
        }
    },
    UENIG_I_NOE_ANNET {
        override fun getFullText(language: LanguageEnum): String {
            return "Jeg er uenig i noe annet i vedtaket mitt"

//            return when(language) {
//                LanguageEnum.NB -> "Jeg er uenig i noe annet i vedtaket mitt"
//                LanguageEnum.EN -> "TEMPLATE ENGLISH UENIG_I_NOE_ANNET"
//            }
        }
    };

    abstract fun getFullText(language: LanguageEnum): String
}