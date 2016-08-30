#!/usr/bin/env bash

set -e
BUILD_NUMBER=${BUILD_NUMBER}
NAME=dbc-glassfish-ims-sink

function die() {
  echo "ERROR: "$@
  exit 1
}

ARTIFACT=dataio-sink-ims-1.0-SNAPSHOT.war
[ -e ${ARTIFACT} ] && rm ${ARTIFACT}
ln ../../../../target/${ARTIFACT} ${ARTIFACT}

TAG=${NAME}-devel
if [ -n "${BUILD_NUMBER}" ] ; then
   TAG=docker-io.dbc.dk/${NAME}:${BUILD_NUMBER}
fi

echo building image with tag ${TAG}

##
time docker build -t ${TAG} -f Dockerfile .
rm ${ARTIFACT}

docker tag ${TAG} ${TAG%:*}:latest