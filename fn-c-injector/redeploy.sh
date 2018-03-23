#!/usr/bin/env bash

set -e -x

APP=fn-c-injector

mvn clean dependency:copy-dependencies compile -DincludeScope=runtime
oc start-build ${APP} --from-dir=. --follow
