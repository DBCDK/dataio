#!/usr/bin/env bash
cd target
docker build -f docker/Dockerfile . -t docker-metascrum.artifacts.dbccloud.dk/dbc-payara-artemis-base:devel || exit 1
