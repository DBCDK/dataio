FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-sink-holdings-items-${project.version}.war docker/sink-holdings-items.json deployments/

