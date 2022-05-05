FROM docker-dbc.artifacts.dbccloud.dk/payara5-micro:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-gui-${project.version}.war docker/gui.json deployments/
