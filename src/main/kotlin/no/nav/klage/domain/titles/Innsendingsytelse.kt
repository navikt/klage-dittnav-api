package no.nav.klage.domain.titles

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema

enum class Innsendingsytelse(val nb: String, val en: String) {
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
    ARBEIDSAVKLARINGSPENGER("Arbeidsavklaringspenger (AAP)", "Work assessment allowance (AAP)"),
    AVTALEFESTET_PENSJON_SPK("Avtalefestet pensjon (AFP) fra Statens pensjonskasse", "Contractual early retirement pension (AFP) in the public sector"),
    AVTALEFESTET_PENSJON_PRIVAT("Avtalefestet pensjon (AFP) i privat sektor", "Contractual early retirement pension (AFP) in the private sector"),
    BARNEBIDRAG_OG_BIDRAGSFORSKUDD(
        "Barnebidrag og bidragsforskudd",
        "Child support (Barnebidrag) and advance payments of child support (Bidragsforskudd)"
    ),
    BARNEPENSJON("Barnepensjon", "Children's pension (Barnepensjon)"),
    BARNETRYGD("Barnetrygd", "Child benefit (Barnetrygd)"),
    BILSTONAD("Stønad til bil og spesialutstyr til kjøretøy", "Vehicle and special equipment for vehicles benefit"),
    DAGPENGER("Dagpenger", "Unemployment benefits (Dagpenger)"),
    //TODO: Fjern når vi ikke lenger har noen drafts, ca. 16. mai 2023.
    DAGPENGER_FERIEPENGER("Feriepenger av dagpenger", "Holiday pay and unemployment benefits (Feriepenger av dagpenger)"),
    DAGPENGER_TILBAKEBETALING_FORSKUDD("Tilbakebetaling av forskudd på dagpenger", "Repayment of advance payment of unemployment benefits (Tilbakebetaling av forskudd på dagpenger)"),
    EKTEFELLEBIDRAG("Ektefellebidrag", "Spousal support (Ektefellebidrag)"),
    ENGANGSSTONAD("Engangsstønad", "Lump-sum grant (Engangsstønad)"),
    ENSLIG_MOR_ELLER_FAR("Enslig mor eller far", "Single mother or father"),
    FORELDREPENGER("Foreldrepenger", "Parental benefit (Foreldrepenger)"),
    GJENLEVENDE(
        "Pensjon til gjenlevende ektefelle eller samboer",
        "Pension to surviving spouse or cohabitant"
    ),
    GRAVFERDSSTONAD("Gravferdsstønad", "Funeral grant (Gravferdsstønad)"),
    GRUNN_OG_HJELPESTONAD("Grunnstønad og hjelpestønad", "Basic benefit (Grunnstønad) and attendance benefit (Hjelpestønad)"),
    HJELPEMIDLER(
        "Hjelpemidler og tilrettelegging ved nedsatt funksjonsevne",
        "Assistive technology and facilitation for impaired functional ability"
    ),
    KONTANTSTOTTE("Kontantstøtte", "Cash-for-care benefit (Kontantstøtte)"),
    KRIGSPENSJON("Krigspensjon", "War pension (Krigspensjon)"),
    LONNSGARANTI("Lønnsgaranti ved konkurs hos arbeidsgiver", "Salary guarantee upon bankruptcy of employer"),
    LONNSKOMPENSASJON("Lønnskompensasjon for permitterte", "Salary compensation for persons who are laid-off"),
    MIDLERTIDIG_KOMPENSASJON(
        "Midlertidig kompensasjon for selvstendig næringsdrivende og frilansere",
        "Temporary compensation for self-employed and freelancers"
    ),
    NAV_LOVEN_14A(
        "Vurdering av behov for bistand etter NAV-loven § 14 a",
        "Assessment of need for assistance according to Section 14 a of the NAV Act"
    ),
    OPPFOSTRINGSBIDRAG("Oppfostringsbidrag", "Upbringing support (Oppfostringsbidrag)"),
    OPPHOLD_ELLER_ARBEID_I_NORGE("Opphold eller arbeid i Norge", "Residence or work in Norway"),
    OPPHOLD_ELLER_ARBEID_UTENFOR_NORGE("Opphold eller arbeid utenfor Norge", "Residence or work outside Norway"),
    SUPPLERENDE_STONAD(
        "Supplerende stønad til personer over 67 år med kort botid i Norge",
        "Supplementary benefit for persons over 67 who have only lived a short period of time in Norway"
    ),
    SUPPLERENDE_STONAD_UFORE_FLYKTNINGER(
        "Supplerende stønad til uføre flyktninger",
        "Supplementary benefit for disabled refugees"
    ),
    SVANGERSKAPSPENGER("Svangerskapspenger", "Pregnancy benefit (Svangerskapspenger)"),
    SYKDOM_I_FAMILIEN(
        "Omsorgspenger, opplæringspenger, pleiepenger",
        "Care benefit (Omsorgspenger), training allowance (Opplæringspenger), attendance allowance (Pleiepenger)"
    ),
    SYKEPENGER("Sykepenger", "Sickness benefit (Sykepenger)"),
    TIDLIGERE_FAMILIEPLEIER("Ytelser til tidligere familiepleier", "Benefits to former family caregivers"),
    TILTAKSPENGER("Tiltakspenger for arbeidsmarkedstiltak", "Benefits (Tiltakspenger) while participating in employment schemes"),
    UFORETRYGD("Uføretrygd", "Disability benefit (Uføretrygd)"),
    YRKESSKADE("Yrkesskade", "Occupational injury (Yrkesskade)");

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
            BARNEPENSJON -> Tema.PEN
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
            SUPPLERENDE_STONAD -> Tema.SUP
            SUPPLERENDE_STONAD_UFORE_FLYKTNINGER -> Tema.SUP
            SVANGERSKAPSPENGER -> Tema.FOR
            SYKDOM_I_FAMILIEN -> Tema.OMS
            SYKEPENGER -> Tema.SYK
            TIDLIGERE_FAMILIEPLEIER -> Tema.PEN
            TILTAKSPENGER -> Tema.IND
            UFORETRYGD -> Tema.UFO
            YRKESSKADE -> Tema.YRK
        }
    }
}