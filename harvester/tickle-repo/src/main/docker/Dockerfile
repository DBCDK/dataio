FROM docker.dbc.dk/payara5-micro:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-harvester-tickle-repo-${project.version}.war docker/harvester-tickle-repo.json deployments/