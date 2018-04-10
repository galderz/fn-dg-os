#!/usr/bin/env bash

set -e -x

export DOCKER_ID_USER="galderz"

mvn clean dependency:copy-dependencies compile -DincludeScope=runtime
docker build -t scavenger-schemaer .

docker tag scavenger-schemaer $DOCKER_ID_USER/scavenger-schemaer
docker push $DOCKER_ID_USER/scavenger-schemaer
