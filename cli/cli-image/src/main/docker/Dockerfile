FROM docker-dbc.artifacts.dbccloud.dk/dbc-java17:latest
LABEL org.opencontainers.image.authors="de-team"

ENV TZ=Europe/Copenhagen

COPY target/dataio-cli-datafile-exporter.jar /work/
COPY target/dataio-cli-job-replicator.jar /work/