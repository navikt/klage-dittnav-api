package no.nav.klage.domain.klage

enum class CheckboxEnum(val fullText: String) {
    AVSLAG_PAA_SOKNAD("Jeg har fått avslag på søknaden min"),
    FOR_LITE_UTBETALT("Jeg har fått for lite utbetalt"),
    UENIG_I_VEDTAK_OM_TILBAKEBETALING("Jeg er uenig i vedtaket om tilbakebetaling"),
    UENIG_I_NOE_ANNET("Jeg er uenig i noe annet i vedtaket mitt")
}

