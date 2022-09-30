FROM docker-metascrum.artifacts.dbccloud.dk/dbc-payara-artemis-base:devel

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-sink-dummy-${project.version}.war docker/sink-dummy.json deployments/
