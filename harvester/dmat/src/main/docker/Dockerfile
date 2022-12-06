FROM docker-dbc.artifacts.dbccloud.dk/payara5-micro:old-202246

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-harvester-dmat-${project.version}.war docker/harvester-dmat.json deployments/
