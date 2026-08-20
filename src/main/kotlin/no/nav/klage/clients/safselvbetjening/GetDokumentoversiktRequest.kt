package no.nav.klage.clients.safselvbetjening

import no.nav.klage.clients.pdl.GraphqlQuery

data class GetDokumentoversiktGraphqlQuery(
    val query: String,
    val variables: GetDokumentoversiktVariables
)

data class GetDokumentoversiktVariables(
    val ident: String,
    val tema: List<Tema> = emptyList(),
)

fun getDokumentoversiktQuery(ident: String): GetDokumentoversiktGraphqlQuery {
    val query = GraphqlQuery::class.java.getResource("/safselvbetjening/getDokumentoversikt.graphql").readText()
        .replace("[\n\r]", "")
    return GetDokumentoversiktGraphqlQuery(
        query = query,
        variables = GetDokumentoversiktVariables(ident = ident)
    )
}

enum class Tema {
    AAP, //Arbeidsavklaringspenger
    AAR, //Aa-registeret
    AGR, //Ajourhold - Grunnopplysninger
    AKT, //Aktivitetsplan med dialoger
    ARP, //Arbeidsrådgivning - psykologtjenester
    ARS, //Arbeidsrådgivning - skjermet
    BAR, //Barnetrygd
    BBF, //Barnebortføring
    BID, //Bidrag
    BIL, //Bil
    DAG, //Dagpenger
    ENF, //Enslig forsørger
    ERS, //Erstatning
    EYB, //Barnepensjon
    EYO, //Omstillingsstønad
    FAR, //Farskap
    FEI, //Feilutbetaling
    FIP, //Fiskerpensjon
    FOR, //Foreldre- og svangerskapspenger
    FOS, //Forsikring
    FRI, //Kompensasjon for selvstendig næringsdrivende/frilansere
    FUL, //Fullmakt
    GEN, //Generell
    GRA, //Gravferdsstønad
    GRU, //Grunn- og hjelpestønad
    HEL, //Helsetjenester og ortopediske hjelpemidler
    HJE, //Hjelpemidler
    IAR, //Inkluderende arbeidsliv
    IND, //Tiltakspenger
    KLL, //Klage - lønnsgaranti
    KON, //Kontantstøtte
    KTA, //Kontroll - anmeldelse
    KTR, //Kontroll
    MED, //Medlemskap
    MOB, //Mobilitetsfremmende stønad
    OLJ, //Oljepionerene
    OMS, //Omsorgspenger, pleiepenger og opplæringspenger
    OPA, //Oppfølging - Arbeidsgiver
    OPP, //Oppfølging
    PAI, //Innsyn
    PEN, //Pensjon
    PER, //Permittering og masseoppsigelser
    POI, //Innsyn etter personopplysningsloven
    REH, //Rehabilitering
    REK, //Rekruttering og stilling
    RPO, //Retting av personopplysninger
    RVE, //Rettferdsvederlag
    SAA, //Sanksjon - Arbeidsgiver
    SAK, //Saksomkostninger
    SAP, //Sanksjon - Person
    SER, //Serviceklager
    SIK, //Sikkerhetstiltak
    STO, //Regnskap/utbetaling
    SUP, //	Supplerende stønad
    SYK, //Sykepenger
    SYM, //Sykmeldinger
    TIL, //Tiltak
    TRK, //Trekkhåndtering
    TRY, //Trygdeavgift
    TSO, //Tilleggsstønad
    TSR, //Tilleggsstønad arbeidssøkere
    UFM, //Unntak fra medlemskap
    UFO, //Uføretrygd
    UKJ, //Ukjent
    UNG, //Ungdomsprogramytelsen
    VEN, //Ventelønn
    YRA, //Yrkesrettet attføring
    YRK //Yrkesskade / Menerstatning
}