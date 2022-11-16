FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:old-202246

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

USER root

RUN apt-get update && apt-get install -qy diffutils colordiff sed libxml2-utils jq

ADD docker/script /bin/
RUN chmod +x /bin/*

USER gfish

COPY dataio-sink-diff-${project.version}.war docker/sink-diff.json deployments/
