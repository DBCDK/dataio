FROM docker.dbc.dk/payara5-full:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-sink-ims-${project.version}.war docker/sink-ims.json deployments/