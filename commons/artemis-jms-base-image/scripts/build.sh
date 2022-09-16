#!/usr/bin/env bash

docker build -f target/docker/Dockerfile . -t docker-metascrum.artifacts.dbccloud.dk:devel || exit 1
