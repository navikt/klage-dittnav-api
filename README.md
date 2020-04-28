# klage-dittnav-api
API for moderniserte klager.

Kotlin-app bygget med bl.a.:
* Spring 
* Exposed (Kotlin ORM)

Flyway kjører ved hver oppstart av applikasjonen og oppdaterer databasen ved ev. endringer.

## Bygge
```
./gradlew bootJar
```
Lager en kjørbar jar-fil.

## Kjøre lokalt

### Kun DB
```
docker-compose up -d klage-dittnav-db
```

Stop/delete:
```
docker-compose down
```

### DB + API
Husk å bygge jar først.

Legg til -d hvis du ikke vil se loggene fra API-et, d.v.s. at applikasjonen kjører i bakgrunnen.
```
docker-compose up --build
```
Stop/delete:
```
docker-compose down
```

## Endepunkter

### App spesifikke
CRUD Rest-API for klager
```
http://localhost:7070/klager
```
### NAIS
Endepunkter som NAIS bruker:
```
http://localhost:7070/internal/health
```

## Metrics
```
http://localhost:7070/internal/prometheus
```
Vi eksponerer (til Prometheus):

* klager.created - hvor mange ganger GET /klager har blitt kalt.

Dette bruker vi til å vise stats i Grafana.

## NAIS/GCP
Appen + db kjører i GCP. For å få dette til har vi fulgt guider fra https://doc.nais.io/

De viktigste punktene for å kjøre i GCP kontra on-prem:

I "application".yaml:
* Spesifisere hvilket namespace appen skal kjøre i (til forskjell fra on-prem der alt kjører i default). For oss er det samme som team-navn (klage).
* Angi riktig(e) ingress(er). For oss (i dev) er dette: https://klage-dittnav-api.dev-gcp.nais.io  

I "Github workflow".yml:
* Angi riktig cluster: f.eks.: dev-gcp

### Hvordan sette opp Postgres
https://doc.nais.io/gcp/postgres

Vi opplevde at det tok litt tid før DB var opprettet og svarte på anrop.
