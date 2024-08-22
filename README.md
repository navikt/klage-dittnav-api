# klage-dittnav-api

API for moderniserte klager.

##Title Keys

Denne appen er i hovedsak backend for https://github.com/navikt/klage-dittnav. 
Denne klienten tilbyr en direkte lenke til opprettelse av klage, der det er mulig å sette feltet `tittel`. 
Verdien på dette feltet må være en av nøklene som er definert i [TitleEnum.kt](src/main/kotlin/no/nav/klage/domain/titles/TitleEnum.kt).

###Nye titler

Dersom du ønsker å bruke en tittel som ikke fins i den definerte listen, så er det mulig å legge til flere. 
Dette kan gjøres ved å opprette en Pull Request til dette repoet der du legger til ønsket tittel i [TitleEnum.kt](src/main/kotlin/no/nav/klage/domain/titles/TitleEnum.kt) på dette formatet:
```
NY_TITTEL("Den nye tittelen på norsk", "The new title in English")
```
Det går også an å kontakte oss på Slack, i kanalen #team-digital-klage.


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

#### Kontakt med PDL

For å få kontakt med PDL er det nødvendig med et autorisasjons-token fra STS (Security Token Service). I disse kallene
settes to headere, `Authorization` og `Nav-Consumer-Token`. Her settes token fra innlogget bruker i `Authorization`, 
mens det er STS-tokenet som skal i den andre. 

##### Windows-spesifikt

For å få kontakt med STS og PDL må det gjøres noen spesifikke grep ved lokal kjøring på Windows. 

###### Port Forwarding

Både STS og PDL kjører i FSS-området, og får å få tilgang til disse må følgende kjøres fra kommanolinje, med kontekst 
satt til `dev-fss`:
```
kubectl port-forward svc/security-token-service 8088:80
```
```
kubectl port-forward svc/pdl-api 7000:80
```

Port på localhost, her `8088` og `7000`, kan være hva du ønsker, men disse skal brukes i andre innstillinger.

Vi har opplevd at disse prosessene ofte avsluttes fordi man mister tilkoblingen, da er det i så fall bare å kjøre dem på
nytt. 

###### Docker Compose

Containere som kjører i Docker på Windows har ikke uten videre tilgang til port forwardingen som settes opp her. For å 
få kjørt appen med Docker Compose kan du spesifisere følgende i `docker-compose.yml`:

```
services:
  klage-dittnav-api:
    ...
    environment:
      ...
      SECURITY_TOKEN_SERVICE_REST_URL: http://host.docker.internal:8088
      PDL_BASE_URL: http://host.docker.internal:7000/graphql
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