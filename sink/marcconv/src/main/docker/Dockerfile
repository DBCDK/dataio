FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:old-202246

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

RUN echo "set server-config.transaction-service.timeout-in-seconds=3600" >>scripts/prebootcommandfile.txt

COPY dataio-sink-marcconv-${project.version}.war docker/sink-marcconv.json deployments/
