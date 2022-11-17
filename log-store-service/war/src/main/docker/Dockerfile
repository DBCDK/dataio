FROM docker-dbc.artifacts.dbccloud.dk/payara5-micro:old-202246

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-log-store-service-war-${project.version}.war docker/logstore.json deployments/
