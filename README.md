# klage-dittnav-api

API for moderniserte klager.

##Title Keys

Denne appen er i hovedsak backend for https://github.com/navikt/klang. Se readme der for informasjon om hvordan du kan ta i bruk lenker til klageløsningen.

##Utvikling

Kotlin-app bygget med bl.a.:
* Spring
* Exposed (Kotlin ORM)

Flyway kjører ved hver oppstart av applikasjonen og oppdaterer databasen ved ev. endringer.

### Bygge
```
./gradlew bootJar
```
Lager en kjørbar jar-fil.

### Kjøre lokalt

#### Kun DB
```
docker-compose up -d klage-dittnav-db
```

Stop/delete:
```
docker-compose down
```

#### DB + API
Husk å bygge jar først.

Legg til -d hvis du ikke vil se loggene fra API-et, d.v.s. at applikasjonen kjører i bakgrunnen.
```
docker-compose up --build
```
Stop/delete:
```
docker-compose down
``` 

### Endepunkter

#### App-spesifikke
CRUD Rest-API for klager
```
http://localhost:7070/klager
```
#### NAIS
Endepunkter som NAIS bruker:
```
http://localhost:7070/internal/health
```

### Metrics
```
http://localhost:7070/internal/prometheus
```
Vi eksponerer (til Prometheus):

* klager.created - hvor mange ganger POST /klager har blitt kalt vellykket.

Dette bruker vi til å vise stats i Grafana.

### NAIS/GCP
Appen + db kjører i GCP. For å få dette til har vi fulgt guider fra https://doc.nais.io/

#### Hvordan sette opp Postgres
https://doc.nais.io/gcp/postgres

Vi opplevde at det tok litt tid før DB var opprettet og svarte på anrop.

#### API
`GET /klager`: Hent alle klager

`POST /klager`: Opprett klage

Eksempel:
```
{
  "foedselsnummer": "012345678910",
  "fritekst": "Tekst her"
}
```

`PUT /klager/{id}`: Endre klage

Eksempel:
```
{
  "id": 1,
  "foedselsnummer": "012345678910",
  "fritekst": "Endret tekst her"
}
```

`DELETE /klager/{id}`: Marker klage som slettet

Eksempel på Klageobjektet:
```
{
  "id": 2,
  "foedselsnummer": "02345678911",
  "fritekst": "Mye tekst her",
  "status": "DRAFT",
  "modifiedByUser": "2020-05-05T15:18:12.686588Z"
}
```

# Linting and verification

This project uses ktlint and detekt for linting and static code analysis. See internal Confluence page for Team Klage for more info.
