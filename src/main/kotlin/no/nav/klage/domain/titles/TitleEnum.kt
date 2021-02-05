package no.nav.klage.domain.titles


enum class TitleEnum(val nb: String, val en: String) {
    AAP("Arbeidsavklaringspenger", "EN_Arbeidsavklaringspenger"),
    AAR("Aa-registeret", "EN_Aa-registeret"),
    AGR("Ajourhold - Grunnopplysninger", "EN_Ajourhold - Grunnopplysninger"),
    BAR("Barnetrygd", "EN_Barnetrygd"),
    BID("1 - Bidrag", "EN_1 - Bidrag"),
    BII("2 - Bidrag innkreving", "EN_2 - Bidrag innkreving"),
    BIL("Bil", "EN_Bil"),
    DAG("Dagpenger", "EN_Dagpenger"),
    ENF("Enslig forsørger", "EN_Enslig forsørger"),
    ERS("Erstatning", "EN_Erstatning"),
    FAR("Farskap", "EN_Farskap"),
    FEI("Feilutbetaling", "EN_Feilutbetaling"),
    FOR("Foreldre- og svangerskapspenger", "EN_Foreldre- og svangerskapspenger"),
    FOS("Forsikring", "EN_Forsikring"),
    FRI(
        "Kompensasjon selvstendig næringsdrivende/frilansere  ",
        "EN_Kompensasjon selvstendig næringsdrivende/frilansere  "
    ),
    FUL("Fullmakt", "EN_Fullmakt"),
    GEN("Generell", "EN_Generell"),
    GRA("Gravferdsstønad", "EN_Gravferdsstønad"),
    GRU("Grunn- og hjelpestønad", "EN_Grunn- og hjelpestønad"),
    HEL("Helsetjenester og ort. hjelpemidler", "EN_Helsetjenester og ort. hjelpemidler"),
    HJE("Hjelpemidler", "EN_Hjelpemidler"),
    IAR("Inkluderende Arbeidsliv ", "EN_Inkluderende Arbeidsliv "),
    IND("Individstønad", "EN_Individstønad"),
    KLA("Klage/Anke", "EN_Klage/Anke"),
    KNA("Kontakt NAV", "EN_Kontakt NAV"),
    KOM("Kommunale tjenester", "EN_Kommunale tjenester"),
    KON("Kontantstøtte", "EN_Kontantstøtte"),
    KTR("Kontroll", "EN_Kontroll"),
    LGA("Lønnsgaranti", "EN_Lønnsgaranti"),
    MED("Medlemskap", "EN_Medlemskap"),
    MOB("Mob.stønad", "EN_Mob.stønad"),
    MOT("3 - Skanning", "EN_3 - Skanning"),
    OKO("Økonomi", "EN_Økonomi"),
    OMS(
        "Omsorgspenger, Pleiepenger og opplæringspenger       ",
        "EN_Omsorgspenger, Pleiepenger og opplæringspenger       "
    ),
    OPA("Oppfølging - Arbeidsgiver", "EN_Oppfølging - Arbeidsgiver"),
    OPP("Oppfølging", "EN_Oppfølging"),
    OVR("4 - Øvrig", "EN_4 - Øvrig"),
    PEN("Pensjon", "EN_Pensjon"),
    PER("Permittering og masseoppsigelser", "EN_Permittering og masseoppsigelser"),
    REH("Rehabilitering", "EN_Rehabilitering"),
    REK("Rekruttering og Stilling", "EN_Rekruttering og Stilling"),
    RPO("Retting av personopplysninger", "EN_Retting av personopplysninger"),
    RVE("Rettferdsvederlag", "EN_Rettferdsvederlag"),
    SAA("Sanksjon - Arbeidsgiver", "EN_Sanksjon - Arbeidsgiver"),
    SAK("Saksomkostning", "EN_Saksomkostning"),
    SAP("Sanksjon - Person", "EN_Sanksjon - Person"),
    SER("Serviceklager", "EN_Serviceklager"),
    SIK("Sikkerhetstiltak", "EN_Sikkerhetstiltak"),
    STO("Regnskap/utbetaling", "EN_Regnskap/utbetaling"),
    SUP("Supplerende stønad", "EN_Supplerende stønad"),
    SYK("Sykepenger", "EN_Sykepenger"),
    SYM("Sykemeldinger", "EN_Sykemeldinger"),
    TIL("Tiltak", "EN_Tiltak"),
    TRK("Trekkhåndtering", "EN_Trekkhåndtering"),
    TRY("Trygdeavgift  ", "EN_Trygdeavgift  "),
    TSO("Tilleggsstønad", "EN_Tilleggsstønad"),
    TSR("Tilleggsstønad arbeidssøkere", "EN_Tilleggsstønad arbeidssøkere"),
    UFM("Unntak fra medlemskap", "EN_Unntak fra medlemskap"),
    UFO("Uføretrygd", "EN_Uføretrygd"),
    UKJ("Ukjent", "EN_Ukjent"),
    VEN("Ventelønn", "EN_Ventelønn"),
    YRA("Yrkesrettet attføring", "EN_Yrkesrettet attføring"),
    YRK("Yrkesskade / Menerstatning", "EN_Yrkesskade / Menerstatning"),

    ALDERSPENSJON("Alderspensjon", "EN_Alderspensjon"),
    ARBEID("Arbeid", "EN_Arbeid"),
    ARBEIDSAVKLARINGSPENGER("Arbeidsavklaringspenger (AAP)", "EN_Arbeidsavklaringspenger (AAP)"),
    AVTALEFESTET_PENSJON("Avtalefestet pensjon (AFP)", "EN_Avtalefestet pensjon (AFP)"),
    BARNEBIDRAG_OG_BIDRAGSFORSKUDD("Barnebidrag og bidragsforskudd", "EN_Barnebidrag og bidragsforskudd"),
    BARNEPENSJON("Barnepensjon", "EN_Barnepensjon"),
    BARNETRYGD("Barnetrygd", "EN_Barnetrygd"),
    BILSTONAD("Stønad til bil og spesialutstyr til kjøretøy", "EN_Stønad til bil og spesialutstyr til kjøretøy"),
    DAGPENGER("Dagpenger", "EN_Dagpenger"),
    EKTEFELLEBIDRAG("Ektefellebidrag", "EN_Ektefellebidrag"),
    ENGANGSSTONAD("Engangsstønad", "EN_Engangsstønad"),
    ENSLIG_MOR_ELLER_FAR("Enslig mor eller far", "EN_Enslig mor eller far"),
    FORELDREPENGER_GENERELL(
        "Foreldrepenger, engangsstønad og svangerskapspenger",
        "EN_Foreldrepenger, engangsstønad og svangerskapspenger"
    ),
    FORELDREPENGER("Foreldrepenger", "EN_Foreldrepenger"),
    GJENLEVENDE(
        "Pensjon til gjenlevende ektefelle eller samboer",
        "EN_Pensjon til gjenlevende ektefelle eller samboer"
    ),
    GRAVFERDSSTONAD("Gravferdsstønad", "EN_Gravferdsstønad"),
    GRUNN_OG_HJELPESTONAD("Grunnstønad og hjelpestønad", "EN_Grunnstønad og hjelpestønad"),
    HJELPEMIDLER(
        "Hjelpemidler og tilrettelegging ved nedsatt funksjonsevne",
        "EN_Hjelpemidler og tilrettelegging ved nedsatt funksjonsevne"
    ),
    KONTANTSTOTTE("Kontantstøtte", "EN_Kontantstøtte"),
    KRIGSPENSJON("Krigspensjon", "EN_Krigspensjon"),
    LONNSGARANTI("Lønnsgaranti ved konkurs hos arbeidsgiver", "EN_Lønnsgaranti ved konkurs hos arbeidsgiver"),
    LONNSKOMPENSASJON("Lønnskompensasjon for permitterte", "EN_Lønnskompensasjon for permitterte"),
    MIDLERTIDIG_KOMPENSASJON(
        "Midlertidig kompensasjon for selvstendig næringsdrivende og frilansere",
        "EN_Midlertidig kompensasjon for selvstendig næringsdrivende og frilansere"
    ),
    NAV_LOVEN_14A(
        "Vurdering av behov for bistand etter NAV loven § 14 a",
        "EN_Vurdering av behov for bistand etter NAV loven § 14 a"
    ),
    OKONOMISK_SOSIALHJELP("Midlertidig økonomisk sosialhjelp", "EN_Midlertidig økonomisk sosialhjelp"),
    OPPFOSTRINGSBIDRAG("Oppfostringsbidrag", "EN_Oppfostringsbidrag"),
    OPPHOLD_ELLER_ARBEID_I_NORGE("Opphold eller arbeid i Norge", "EN_Opphold eller arbeid i Norge"),
    OPPHOLD_ELLER_ARBEID_UTENFOR_NORGE("Opphold eller arbeid utenfor Norge", "EN_Opphold eller arbeid utenfor Norge"),
    SUPPLERENDE_STONAD(
        "Supplerende stønad til pensjon ved kort botid i Norge",
        "EN_Supplerende stønad til pensjon ved kort botid i Norge"
    ),
    SVANGERSKAPSPENGER("Svangerskapspenger", "EN_Svangerskapspenger"),
    SYKDOM_I_FAMILIEN(
        "Omsorgspenger, opplæringspenger, pleiepenger",
        "EN_Omsorgspenger, opplæringspenger, pleiepenger"
    ),
    SYKEPENGER("Sykepenger", "EN_Sykepenger"),
    TIDLIGERE_FAMILIEPLEIER("Ytelser til tidligere familiepleier", "EN_Ytelser til tidligere familiepleier"),
    TILTAKSPENGER("Tiltakspenger for arbeidsrettet tiltak", "EN_Tiltakspenger for arbeidsrettet tiltak"),
    UFORETRYGD("Uføretrygd", "EN_Uføretrygd"),
    YRKESSKADETRYGD("Yrkesskade og yrkesskadetrygd", "EN_Yrkesskade og yrkesskadetrygd"),
    FEIL("Her har det skjedd noe feil.", "EN_Her har det skjedd noe feil");

    companion object {
        private val map = TitleEnum.values().associateBy(TitleEnum::nb)
        fun getTitleKeyFromNbTitle(ytelse: String) = map[ytelse.capitalize()]
    }
}