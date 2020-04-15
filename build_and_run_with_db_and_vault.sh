#!/bin/bash

./gradlew build shadowJar

#stop all running docker instances
echo "Take down old containers"
docker-compose down

#start vault
echo "Start vault"
docker-compose up -d --build vault
echo "vault started"

#insert secrets
echo "sleep for 3 s"
sleep 3
export VAULT_ADDR=http://localhost:8200

vault login 123456789
vault secrets enable -path=dev/creds kv
vault write dev/creds/klage-user username="klage-user" password="klage-user"
vault write dev/creds/klage-admin username="klage-admin" password="klage-admin"
echo "secrets created"

#start db and api
echo "starting api and db"
# when running api in Intellij: docker-compose up --build klage-dittnav-db
docker-compose up --build klage-dittnav-api
