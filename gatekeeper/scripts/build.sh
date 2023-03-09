#!/bin/bash -e
cd target

cp jmx_prometheus_httpserver-*.jar docker/jmx-exporter/
docker build docker/jmx-exporter -t docker-metascrum.artifacts.dbccloud.dk/gatekeeper-jmx-exporter:devel
