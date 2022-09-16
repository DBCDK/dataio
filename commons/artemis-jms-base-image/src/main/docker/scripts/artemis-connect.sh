#!/usr/bin/env bash

function die() {
  error "$@"
  exit 1
}

if [ -z "$ARTEMIS_MQ_HOST" ]
then
  die "Missing ARTEMIS_MQ_HOST. Unable to start."
fi

envsubst '$ARTEMIS_MQ_HOST' < scripts/artemis-connection-setup.txt >> scripts/postbootcommandfile.txt
