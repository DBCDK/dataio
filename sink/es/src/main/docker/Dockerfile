FROM docker.dbc.dk/payara5-full:104

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-sink-es-${project.version}.war docker/sink-es.json deployments/