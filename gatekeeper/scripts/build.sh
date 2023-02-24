#!/bin/bash -e
cd target
cp dataio-gatekeeper-*.jar docker/gatekeeper-proftp/
docker build docker/gatekeeper-proftp -t docker-metascrum.artifacts.dbccloud.dk/gatekeeper-staging:devel

cp jmx_prometheus_httpserver-*.jar docker/jmx-exporter/
docker build docker/jmx-exporter -t docker-metascrum.artifacts.dbccloud.dk/jmx-exporter:devel
