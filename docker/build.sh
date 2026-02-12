#!/usr/bin/env bash
set -eo pipefail

rm -f target/docker.out

usage() {
  cat <<EOF
Builds docker image using target/docker/Dockerfile.
Automatically sets docker tag.
Usage:
  $0 --docker-repo DOCKER_REPO
EOF
}

# -------------------- Args --------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    --docker-repo) DOCKER_REPO="$2"; shift 2;;
    -h|--help) usage; exit 0;;
    *) echo "Unknown arg: $1"; usage; exit 1;;
  esac
done

function is_ci_server() {
  if [[ -n "$JENKINS_URL" && -n "$BUILD_NUMBER" && -n "$BRANCH_NAME" ]]; then
    return 0  # true
  else
    return 1  # false
  fi
}

DOCKER_TAG=devel
if is_ci_server; then
  DOCKER_TAG=$BRANCH_NAME-$BUILD_NUMBER
  if [[ "$BRANCH_NAME" = "master" ]]; then
      DOCKER_TAG=$BUILD_NUMBER
  fi
fi

docker pull docker-dbc.artifacts.dbccloud.dk/payara6-micro:latest
echo "building docker image: $DOCKER_REPO:$DOCKER_TAG"
docker_args=(build -f target/docker/Dockerfile . -t "$DOCKER_REPO:$DOCKER_TAG")
echo "docker ${docker_args[*]}"
docker "${docker_args[@]}"
echo "$DOCKER_REPO:$DOCKER_TAG" > target/docker.out
