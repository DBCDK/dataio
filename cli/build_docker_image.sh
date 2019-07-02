#!/usr/bin/env bash

set -e
REGISTRY=docker-io.dbc.dk
NAME=dataio-cli
TIMEFORMAT="time: ${NAME} e: %E U: %U S: %S P: %P "

if [[ -n "${SKIP_BUILD_DOCKER_IMAGE}" ]]; then
  echo skipping building of ${NAME} docker image
  exit 0
fi

RETURN_DIR="$(pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

TAG=devel
if [ -n "${BUILD_NUMBER}" ] ; then
  TAG=${BUILD_NUMBER}
  test ! -z ${BRANCH_NAME} && TAG=${BRANCH_NAME}-${TAG}
fi
IMAGE=${REGISTRY}/${NAME}:${TAG}

echo building ${IMAGE} docker image

time docker build -t ${IMAGE} --build-arg build_number=${BUILD_NUMBER:=devel} --build-arg git_commit=${GIT_COMMIT:=devel} -f Dockerfile --pull --no-cache .

echo ${REGISTRY}/${NAME} >> ../docker-images.log

cd "${RETURN_DIR}"
