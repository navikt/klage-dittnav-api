package no.nav.klage.domain.titles

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema

enum class Innsendingsytelse(val nb: String, val en: String, val nn: String) {
    AAP("Arbeidsavklaringspenger", "Work assessment allowance (AAP)", "Arbeidsavklaringspengar"),
    AAR("Aa-registeret", "Aa register", "Aa-registeret"),
    AGR("Ajourhold - Grunnopplysninger", "Ajourhold - Grunnopplysninger", "Ajourhald - Grunnopplysningar"),
    BAR("Barnetrygd", "Child benefit", "Barnetrygd"),
    BID("1 - Bidrag", "1 - Bidrag", "1 - Bidrag"),
    BII("2 - Bidrag innkreving", "2 - Bidrag innkreving", "2 - Bidrag innkrevjing"),
    BIL("Bil", "Vehicle", "Bil"),
    DAG("Dagpenger", "Unemployment benefits", "Dagpengar"),
    ENF("Enslig forsørger", "Single provider", "Enslig forsørgjar"),
    ERS("Erstatning", "Compensation", "Erstatning"),
    FAR("Farskap", "Paternity", "Farskap"),
    FEI("Feilutbetaling", "Feilutbetaling", "Feilutbetaling"),
    FOR("Foreldre- og svangerskapspenger", "Parental benefit and pregnancy benefit", "Foreldre- og svangerskapspengar"),
    FOS("Forsikring", "Insurance", "Forsikring"),
    FRI(
        "Kompensasjon selvstendig næringsdrivende/frilansere",
        "Compensation for self-employed and freelancers",
        "Kompensasjon sjølvstendig næringsdrivande/frilansarar"
    ),
    FUL("Fullmakt", "Power of attorney", "Fullmakt"),
    GEN("Generell", "General", "Generell"),
    GRA("Gravferdsstønad", "Funeral grant", "Gravferdsstønad"),
    GRU("Grunn- og hjelpestønad", "Basic benefit and assistance allowance", "Grunn- og hjelpestønad"),
    HEL("Helsetjenester og ort. hjelpemidler", "Helsetjenester og ort. hjelpemidler", "Helsetenester og ort. hjelpemiddel"),
    HJE("Hjelpemidler", "Assistive technology", "Hjelpemiddel"),
    IAR("Inkluderende Arbeidsliv", "Inclusive employment", "Inkluderande Arbeidsliv"),
    IND("Individstønad", "Individstønad", "Individstønad"),
    KLA("Klage/Anke", "Complaint/appeal", "Klage/Anke"),
    KNA("Kontakt NAV", "Contact NAV", "Kontakt NAV"),
    KOM("Kommunale tjenester", "Municipal services", "Kommunale tenester"),
    KON("Kontantstøtte", "Cash-for-care benefit", "Kontantstøtte"),
    KTR("Kontroll", "Control", "Kontroll"),
    LGA("Lønnsgaranti", "Salary guarantee", "Lønnsgaranti"),
    MED("Medlemskap", "Membership", "Medlemskap"),
    MOB("Mob.stønad", "Mob.stønad", "Mob.stønad"),
    MOT("3 - Skanning", "3 - Scanning", "3 - Skanning"),
    OKO("Økonomi", "Økonomi", "Økonomi"),
    OMS(
        "Omsorgspenger, Pleiepenger og opplæringspenger",
        "Care benefit, training allowance, attendance allowance",
        "Omsorgspengar, Pleiepengar og opplæringspengar"
    ),
    OPA("Oppfølging - Arbeidsgiver", "Oppfølging - Arbeidsgiver", "Oppfølging - Arbeidsgivar"),
    OPP("Oppfølging", "Oppfølging", "Oppfølging"),
    OVR("4 - Øvrig", "4 - Other", "4 - Anna"),
    PEN("Pensjon", "Pension", "Pensjon"),
    PER("Permittering og masseoppsigelser", "Permittering og masseoppsigelser", "Permittering og masseoppseiingar"),
    REH("Rehabilitering", "Rehabilitation", "Rehabilitering"),
    REK("Rekruttering og Stilling", "Rekruttering og Stilling", "Rekruttering og Stilling"),
    RPO("Retting av personopplysninger", "Retting av personopplysninger", "Retting av personopplysningar"),
    RVE("Rettferdsvederlag", "Rettferdsvederlag", "Rettferdsvederlag"),
    SAA("Sanksjon - Arbeidsgiver", "Sanksjon - Arbeidsgiver", "Sanksjon - Arbeidsgivar"),
    SAK("Saksomkostning", "Costs", "Sakskostnad"),
    SAP("Sanksjon - Person", "Sanksjon - Person", "Sanksjon - Person"),
    SER("Serviceklager", "Service complaints", "Serviceklager"),
    SIK("Sikkerhetstiltak", "Sikkerhetstiltak", "Sikkerhetstiltak"),
    STO("Regnskap/utbetaling", "Regnskap/utbetaling", "Rekneskap/utbetaling"),
    SUP("Supplerende stønad", "Supplementary benefit", "Supplerande stønad"),
    SYK("Sykepenger", "Sickness benefit", "Sjukepengar"),
    SYM("Sykemeldinger", "Sick note", "Sjukmeldingar"),
    TIL("Tiltak", "Tiltak", "Tiltak"),
    TRK("Trekkhåndtering", "Trekkhåndtering", "Trekkhåndtering"),
    TRY("Trygdeavgift", "National Insurance contribution", "Trygdeavgift"),
    TSO("Tilleggsstønad", "Supplemental benefits", "Tilleggsstønad"),
    TSR("Tilleggsstønad arbeidssøkere", "Supplemental benefits job seekers", "Tilleggsstønad arbeidssøkarar"),
    UFM("Unntak fra medlemskap", "Membership exception", "Unntak frå medlemskap"),
    UFO("Uføretrygd", "Disability benefit", "Uføretrygd"),
    UKJ("Ukjent", "Unknown", "Ukjent"),
    VEN("Ventelønn", "Ventelønn", "Ventelønn"),
    YRA("Yrkesrettet attføring", "Yrkesrettet attføring", "Yrkesretta attføring"),
    YRK("Yrkesskade / Menerstatning", "Occupational injury / Menerstatning", "Yrkesskade / Menerstatning"),

    ALDERSPENSJON("Alderspensjon", "Retirement pension", "Alderspensjon"),
    ARBEIDSAVKLARINGSPENGER("Arbeidsavklaringspenger (AAP)", "Work assessment allowance (AAP)", "Arbeidsavklaringspengar (AAP)"),
    AVTALEFESTET_PENSJON_SPK("Avtalefestet pensjon (AFP) fra Statens pensjonskasse", "Contractual early retirement pension (AFP) in the public sector", "Avtalefest pensjon (AFP) frå Statens pensjonskasse"),
    AVTALEFESTET_PENSJON_PRIVAT("Avtalefestet pensjon (AFP) i privat sektor", "Contractual early retirement pension (AFP) in the private sector", "Avtalefest pensjon (AFP) i privat sektor"),
    BARNEBIDRAG_OG_BIDRAGSFORSKUDD(
        "Barnebidrag og bidragsforskudd",
        "Child support (Barnebidrag) and advance payments of child support (Bidragsforskudd)",
        "Barnebidrag og bidragsforskot"
    ),
    BARNEPENSJON("Barnepensjon", "Children's pension (Barnepensjon)", "Barnepensjon"),
    BARNETRYGD("Barnetrygd", "Child benefit (Barnetrygd)", "Barnetrygd"),
    BILSTONAD("Stønad til bil og spesialutstyr til kjøretøy", "Vehicle and special equipment for vehicles benefit", "Stønad til bil og spesialutstyr til køyretøy"),
    DAGPENGER("Dagpenger", "Unemployment benefits (Dagpenger)", "Dagpengar"),
    //TODO: Fjern når vi ikke lenger har noen drafts, ca. 16. mai 2023.
    DAGPENGER_FERIEPENGER("Feriepenger av dagpenger", "Holiday pay and unemployment benefits (Feriepenger av dagpenger)", "Feriepengar av dagpengar"),
    DAGPENGER_TILBAKEBETALING_FORSKUDD("Tilbakebetaling av forskudd på dagpenger", "Repayment of advance payment of unemployment benefits (Tilbakebetaling av forskudd på dagpenger)", "Tilbakebetaling av forskot på dagpengar"),
    EKTEFELLEBIDRAG("Ektefellebidrag", "Spousal support (Ektefellebidrag)", "Ektefellebidrag"),
    ENGANGSSTONAD("Engangsstønad", "Lump-sum grant (Engangsstønad)", "Eingongsstønad"),
    ENSLIG_MOR_ELLER_FAR("Enslig mor eller far", "Single mother or father", "Enslig mor eller far"),
    FORELDREPENGER("Foreldrepenger", "Parental benefit (Foreldrepenger)", "Foreldrepengar"),
    GJENLEVENDE(
        "Pensjon til gjenlevende ektefelle eller samboer",
        "Pension to surviving spouse or cohabitant",
        "Pensjon til gjenlevande ektefelle eller sambuar"
    ),
    GRAVFERDSSTONAD("Gravferdsstønad", "Funeral grant (Gravferdsstønad)", "Gravferdsstønad"),
    GRUNN_OG_HJELPESTONAD("Grunnstønad og hjelpestønad", "Basic benefit (Grunnstønad) and attendance benefit (Hjelpestønad)", "Grunnstønad og hjelpestønad"),
    HJELPEMIDLER(
        "Hjelpemidler og tilrettelegging ved nedsatt funksjonsevne",
        "Assistive technology and facilitation for impaired functional ability",
        "Hjelpemiddel og tilrettelegging ved nedsatt funksjonsevne"
    ),
    KONTANTSTOTTE("Kontantstøtte", "Cash-for-care benefit (Kontantstøtte)", "Kontantstøtte"),
    KRIGSPENSJON("Krigspensjon", "War pension (Krigspensjon)", "Krigspensjon"),
    LONNSGARANTI("Lønnsgaranti ved konkurs hos arbeidsgiver", "Salary guarantee upon bankruptcy of employer", "Lønnsgaranti ved konkurs hjå arbeidsgivar"),
    LONNSKOMPENSASJON("Lønnskompensasjon for permitterte", "Salary compensation for persons who are laid-off", "Lønnskompensasjon for permitterte"),
    MIDLERTIDIG_KOMPENSASJON(
        "Midlertidig kompensasjon for selvstendig næringsdrivende og frilansere",
        "Temporary compensation for self-employed and freelancers",
        "Midlertidig kompensasjon for sjølvstendig næringsdrivande og frilansarar"
    ),
    NAV_LOVEN_14A(
        "Vurdering av behov for bistand etter NAV-loven § 14 a",
        "Assessment of need for assistance according to Section 14 a of the NAV Act",
        "Vurdering av behov for bistand etter NAV-lova § 14 a"
    ),
    OPPFOSTRINGSBIDRAG("Oppfostringsbidrag", "Upbringing support (Oppfostringsbidrag)", "Oppfostringsbidrag"),
    OPPHOLD_ELLER_ARBEID_I_NORGE("Opphold eller arbeid i Norge", "Residence or work in Norway", "Opphald eller arbeid i Noreg"),
    OPPHOLD_ELLER_ARBEID_UTENFOR_NORGE("Opphold eller arbeid utenfor Norge", "Residence or work outside Norway", "Opphald eller arbeid utanfor Noreg"),
    OMSTILLINGSSTOENAD("Omstillingsstønad", "Adjustment allowance (Omstillingsstønad)", "Omstillingsstønad"),
    REISEKOSTNADER_VED_SAMVAER("Reisekostnader ved samvær", "Travel costs for visits", "Reisekostnader ved samvær"),
    SUPPLERENDE_STONAD(
        "Supplerende stønad til personer over 67 år med kort botid i Norge",
        "Supplementary benefit for persons over 67 who have only lived a short period of time in Norway",
        "Supplerande stønad til personar over 67 år med kort butid i Noreg"
    ),
    SUPPLERENDE_STONAD_UFORE_FLYKTNINGER(
        "Supplerende stønad til uføre flyktninger",
        "Supplementary benefit for disabled refugees",
        "Supplerande stønad til uføre flyktningar"
    ),
    SVANGERSKAPSPENGER("Svangerskapspenger", "Pregnancy benefit (Svangerskapspenger)", "Svangerskapspengar"),
    SYKDOM_I_FAMILIEN(
        "Omsorgspenger, opplæringspenger, pleiepenger",
        "Care benefit (Omsorgspenger), training allowance (Opplæringspenger), attendance allowance (Pleiepenger)",
        "Omsorgspengar, opplæringspengar, pleiepengar"
    ),
    SYKEPENGER("Sykepenger", "Sickness benefit (Sykepenger)", "Sjukepengar"),
    TIDLIGERE_FAMILIEPLEIER("Ytelser til tidligere familiepleier", "Benefits to former family caregivers", "Ytingar til tidlegare familiepleiarar"),
    TILLEGGSSTONADER("Tilleggsstønader", "Supplemental benefits", "Tilleggsstønader"),
    TILTAKSPENGER("Tiltakspenger for arbeidsmarkedstiltak", "Benefits (Tiltakspenger) while participating in employment schemes", "Tiltakspengar for arbeidsmarknadstiltak"),
    UFORETRYGD("Uføretrygd", "Disability benefit (Uføretrygd)", "Uføretrygd"),
    YRKESSKADE("Yrkesskade", "Occupational injury (Yrkesskade)", "Yrkesskade");

    fun getChosenTitle(language: LanguageEnum): String {
        return when (language) {
            LanguageEnum.NB -> this.nb
            LanguageEnum.EN -> this.en
            LanguageEnum.NN -> this.nn
        }
    }

    data class TitleInAllLanguages(
        val nb: String,
        val en: String,
        val nn: String,
    )

    fun getTitleInAllLanguages(): TitleInAllLanguages {
        return TitleInAllLanguages(
            this.nb,
            this.en,
            this.nn,
        )
    }

    fun toTema(): Tema {
        return when(this) {
            AAP -> Tema.AAP
            AAR -> Tema.AAR
            AGR -> Tema.AGR
            BAR -> Tema.BAR
            BID -> Tema.BID
            BII -> Tema.BII
            BIL -> Tema.BIL
            DAG -> Tema.DAG
            ENF -> Tema.ENF
            ERS -> Tema.ERS
            FAR -> Tema.FAR
            FEI -> Tema.FEI
            FOR -> Tema.FOR
            FOS -> Tema.FOS
            FRI -> Tema.FRI
            FUL -> Tema.FUL
            GEN -> Tema.GEN
            GRA -> Tema.GRA
            GRU -> Tema.GRU
            HEL -> Tema.HEL
            HJE -> Tema.HJE
            IAR -> Tema.IAR
            IND -> Tema.IND
            KLA -> Tema.KLA
            KNA -> Tema.KNA
            KOM -> Tema.KOM
            KON -> Tema.KON
            KTR -> Tema.KTR
            LGA -> Tema.LGA
            MED -> Tema.MED
            MOB -> Tema.MOB
            MOT -> Tema.MOT
            OKO -> Tema.OKO
            OMS -> Tema.OMS
            OPA -> Tema.OPA
            OPP -> Tema.OPP
            OVR -> Tema.OVR
            PEN -> Tema.PEN
            PER -> Tema.PER
            REH -> Tema.REH
            REK -> Tema.REK
            RPO -> Tema.RPO
            RVE -> Tema.RVE
            SAA -> Tema.SAA
            SAK -> Tema.SAK
            SAP -> Tema.SAP
            SER -> Tema.SER
            SIK -> Tema.SIK
            STO -> Tema.STO
            SUP -> Tema.SUP
            SYK -> Tema.SYK
            SYM -> Tema.SYM
            TIL -> Tema.TIL
            TRK -> Tema.TRK
            TRY -> Tema.TRY
            TSO -> Tema.TSO
            TSR -> Tema.TSR
            UFM -> Tema.UFM
            UFO -> Tema.UFO
            UKJ -> Tema.UKJ
            VEN -> Tema.VEN
            YRA -> Tema.YRA
            YRK -> Tema.YRK
            ALDERSPENSJON -> Tema.PEN
            ARBEIDSAVKLARINGSPENGER -> Tema.AAP
            AVTALEFESTET_PENSJON_SPK -> Tema.PEN
            AVTALEFESTET_PENSJON_PRIVAT -> Tema.PEN
            BARNEBIDRAG_OG_BIDRAGSFORSKUDD -> Tema.BID
            BARNEPENSJON -> Tema.EYB
            BARNETRYGD -> Tema.BAR
            BILSTONAD -> Tema.BIL
            DAGPENGER -> Tema.DAG
            DAGPENGER_FERIEPENGER -> Tema.DAG
            DAGPENGER_TILBAKEBETALING_FORSKUDD -> Tema.DAG
            EKTEFELLEBIDRAG -> Tema.BID
            ENGANGSSTONAD -> Tema.FOR
            ENSLIG_MOR_ELLER_FAR -> Tema.ENF
            FORELDREPENGER -> Tema.FOR
            GJENLEVENDE -> Tema.PEN
            GRAVFERDSSTONAD -> Tema.GRA
            GRUNN_OG_HJELPESTONAD -> Tema.GRU
            HJELPEMIDLER -> Tema.HJE
            KONTANTSTOTTE -> Tema.KON
            KRIGSPENSJON -> Tema.PEN
            LONNSGARANTI -> Tema.GEN
            LONNSKOMPENSASJON -> Tema.DAG
            MIDLERTIDIG_KOMPENSASJON -> Tema.GEN
            NAV_LOVEN_14A -> Tema.OPP
            OPPFOSTRINGSBIDRAG -> Tema.BID
            OPPHOLD_ELLER_ARBEID_I_NORGE -> Tema.MED
            OPPHOLD_ELLER_ARBEID_UTENFOR_NORGE -> Tema.MED
            OMSTILLINGSSTOENAD -> Tema.EYO
            REISEKOSTNADER_VED_SAMVAER -> Tema.BID
            SUPPLERENDE_STONAD -> Tema.SUP
            SUPPLERENDE_STONAD_UFORE_FLYKTNINGER -> Tema.SUP
            SVANGERSKAPSPENGER -> Tema.FOR
            SYKDOM_I_FAMILIEN -> Tema.OMS
            SYKEPENGER -> Tema.SYK
            TIDLIGERE_FAMILIEPLEIER -> Tema.PEN
            TILLEGGSSTONADER -> Tema.TSO
            TILTAKSPENGER -> Tema.IND
            UFORETRYGD -> Tema.UFO
            YRKESSKADE -> Tema.YRK
        }
    }
}