#!/usr/bin/env bash

set -e -x

APP=vertx-player

mvn clean package -DskipTests=true; oc start-build ${APP} --from-dir=. --follow
