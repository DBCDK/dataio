FROM docker-dbc.artifacts.dbccloud.dk/payara5-micro:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-harvester-rr-${project.version}.war docker/harvester-rr.json deployments/
