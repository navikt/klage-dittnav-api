package no.nav.klage.domain.titles

import no.nav.klage.domain.LanguageEnum

enum class TitleEnum(val nb: String, val en: String) {
    AAP("Arbeidsavklaringspenger", "Work assessment allowance (AAP)"),
    AAR("Aa-registeret", "Aa register"),
    AGR("Ajourhold - Grunnopplysninger", "Ajourhold - Grunnopplysninger"),
    BAR("Barnetrygd", "Child benefit"),
    BID("1 - Bidrag", "1 - Bidrag"),
    BII("2 - Bidrag innkreving", "2 - Bidrag innkreving"),
    BIL("Bil", "Vehicle"),
    DAG("Dagpenger", "Unemployment benefits"),
    ENF("Enslig forsørger", "Single provider"),
    ERS("Erstatning", "Compensation"),
    FAR("Farskap", "Paternity"),
    FEI("Feilutbetaling", "Feilutbetaling"),
    FOR("Foreldre- og svangerskapspenger", "Parental benefit and pregnancy benefit"),
    FOS("Forsikring", "Insurance"),
    FRI(
        "Kompensasjon selvstendig næringsdrivende/frilansere",
        "Compensation for self-employed and freelancers"
    ),
    FUL("Fullmakt", "Power of attorney"),
    GEN("Generell", "General"),
    GRA("Gravferdsstønad", "Funeral grant"),
    GRU("Grunn- og hjelpestønad", "Basic benefit and assistance allowance"),
    HEL("Helsetjenester og ort. hjelpemidler", "Helsetjenester og ort. hjelpemidler"),
    HJE("Hjelpemidler", "Assistive technology"),
    IAR("Inkluderende Arbeidsliv", "Inclusive employment"),
    IND("Individstønad", "Individstønad"),
    KLA("Klage/Anke", "Complaint/appeal"),
    KNA("Kontakt NAV", "Contact NAV"),
    KOM("Kommunale tjenester", "Municipal services"),
    KON("Kontantstøtte", "Cash-for-care benefit"),
    KTR("Kontroll", "Control"),
    LGA("Lønnsgaranti", "Salary guarantee"),
    MED("Medlemskap", "Membership"),
    MOB("Mob.stønad", "Mob.stønad"),
    MOT("3 - Skanning", "3 - Scanning"),
    OKO("Økonomi", "Økonomi"),
    OMS(
        "Omsorgspenger, Pleiepenger og opplæringspenger",
        "Care benefit, training allowance, attendance allowance"
    ),
    OPA("Oppfølging - Arbeidsgiver", "Oppfølging - Arbeidsgiver"),
    OPP("Oppfølging", "Oppfølging"),
    OVR("4 - Øvrig", "4 - Other"),
    PEN("Pensjon", "Pension"),
    PER("Permittering og masseoppsigelser", "Permittering og masseoppsigelser"),
    REH("Rehabilitering", "Rehabilitation"),
    REK("Rekruttering og Stilling", "Rekruttering og Stilling"),
    RPO("Retting av personopplysninger", "Retting av personopplysninger"),
    RVE("Rettferdsvederlag", "Rettferdsvederlag"),
    SAA("Sanksjon - Arbeidsgiver", "Sanksjon - Arbeidsgiver"),
    SAK("Saksomkostning", "Costs"),
    SAP("Sanksjon - Person", "Sanksjon - Person"),
    SER("Serviceklager", "Service complaints"),
    SIK("Sikkerhetstiltak", "Sikkerhetstiltak"),
    STO("Regnskap/utbetaling", "Regnskap/utbetaling"),
    SUP("Supplerende stønad", "Supplementary benefit"),
    SYK("Sykepenger", "Sickness benefit"),
    SYM("Sykemeldinger", "Sick note"),
    TIL("Tiltak", "Tiltak"),
    TRK("Trekkhåndtering", "Trekkhåndtering"),
    TRY("Trygdeavgift", "National Insurance contribution"),
    TSO("Tilleggsstønad", "Supplemental benefits"),
    TSR("Tilleggsstønad arbeidssøkere", "Supplemental benefits job seekers"),
    UFM("Unntak fra medlemskap", "Membership exception"),
    UFO("Uføretrygd", "Disability benefit"),
    UKJ("Ukjent", "Unknown"),
    VEN("Ventelønn", "Ventelønn"),
    YRA("Yrkesrettet attføring", "Yrkesrettet attføring"),
    YRK("Yrkesskade / Menerstatning", "Occupational injury / Menerstatning"),

    ALDERSPENSJON("Alderspensjon", "Old-age pension"),
    ARBEID("Arbeid", "Work"),
    ARBEIDSAVKLARINGSPENGER("Arbeidsavklaringspenger (AAP)", "Work assessment allowance (AAP)"),
    AVTALEFESTET_PENSJON("Avtalefestet pensjon (AFP)", "Contractual pension from The Norwegian Public Service Pension Fund"),
    BARNEBIDRAG_OG_BIDRAGSFORSKUDD(
        "Barnebidrag og bidragsforskudd",
        "Child support and advance payment"
    ),
    BARNEPENSJON("Barnepensjon", "Child who has lost one or both parents"),
    BARNETRYGD("Barnetrygd", "Child benefit"),
    BILSTONAD("Stønad til bil og spesialutstyr til kjøretøy", "Car (motor vehicle)"),
    DAGPENGER("Dagpenger", "Unemployment benefits"),
    EKTEFELLEBIDRAG("Ektefellebidrag", "Alimony"),
    ENGANGSSTONAD("Engangsstønad", "Lump-sum grant"),
    ENSLIG_MOR_ELLER_FAR("Enslig mor eller far", "Single mother or father"),
    FORELDREPENGER_GENERELL(
        "Foreldrepenger, engangsstønad og svangerskapspenger",
        "Parental benefit, lump-sum grant and pregnancy benefit"
    ),
    FORELDREPENGER("Foreldrepenger", "Parental benefit"),
    GJENLEVENDE(
        "Pensjon til gjenlevende ektefelle eller samboer",
        "Surviving spouse, partner or cohabitant"
    ),
    GRAVFERDSSTONAD("Gravferdsstønad", "Grant for funeral and coffin transportation arrangements"),
    GRUNN_OG_HJELPESTONAD("Grunnstønad og hjelpestønad", "Basic benefit and attendance benefit"),
    HJELPEMIDLER(
        "Hjelpemidler og tilrettelegging ved nedsatt funksjonsevne",
        "Various"
    ),
    KONTANTSTOTTE("Kontantstøtte", "Cash-for-care benefit"),
    KRIGSPENSJON("Krigspensjon", "Pension, war veterans"),
    LONNSGARANTI("Lønnsgaranti ved konkurs hos arbeidsgiver", "Salary guarantee upon bankruptcy of employer"),
    LONNSKOMPENSASJON("Lønnskompensasjon for permitterte", "Salary compensation for persons who are laid-off"),
    MIDLERTIDIG_KOMPENSASJON(
        "Midlertidig kompensasjon for selvstendig næringsdrivende og frilansere",
        "Temporary compensation for self-employed and freelancers"
    ),
    NAV_LOVEN_14A(
        "Vurdering av behov for bistand etter NAV loven § 14 a",
        "Assessment of need for assistance according to Section 14 a of the NAV Act"
    ),
    OKONOMISK_SOSIALHJELP("Midlertidig økonomisk sosialhjelp", "Temporary financial social assistance"),
    OPPFOSTRINGSBIDRAG("Oppfostringsbidrag", "Child support / upbringing support"),
    OPPHOLD_ELLER_ARBEID_I_NORGE("Opphold eller arbeid i Norge", "Stay or work in Norway or on Norwegian continental shelf"),
    OPPHOLD_ELLER_ARBEID_UTENFOR_NORGE("Opphold eller arbeid utenfor Norge", "Stay or work outside Norway / certificates of coverage"),
    SUPPLERENDE_STONAD(
        "Supplerende stønad til pensjon ved kort botid i Norge",
        "Supplementary benefits for people over 67 years"
    ),
    SVANGERSKAPSPENGER("Svangerskapspenger", "Pregnancy benefit"),
    SYKDOM_I_FAMILIEN(
        "Omsorgspenger, opplæringspenger, pleiepenger",
        "Care benefit, training allowance, attendance allowance"
    ),
    SYKEPENGER("Sykepenger", "Sickness benefit"),
    TIDLIGERE_FAMILIEPLEIER("Ytelser til tidligere familiepleier", "Family carer"),
    TILTAKSPENGER("Tiltakspenger for arbeidsmarkedstiltak", "Benefits while participating in employment schemes"),
    UFORETRYGD("Uføretrygd", "Disability benefit"),
    YRKESSKADE("Yrkesskade", "Occupational injury"),
    FEIL("Her har det skjedd noe feil.", "Error");

    companion object {
        private val map = TitleEnum.values().associateBy(TitleEnum::nb)
        fun getTitleKeyFromNbTitle(ytelse: String) = map[ytelse.capitalize()]
    }

    fun getChosenTitle(language: LanguageEnum): String {
        return when (language) {
            LanguageEnum.NB -> this.nb
            LanguageEnum.EN -> this.en
        }
    }

    data class TitleInAllLanguages(
        val nb: String,
        val en: String
    )

    fun getTitleInAllLanguages(): TitleInAllLanguages {
        return TitleInAllLanguages(
            this.nb,
            this.en
        )
    }
}