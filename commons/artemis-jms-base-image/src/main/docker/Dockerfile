FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit
ARG artemisVersion=2.23.0
COPY --chown=gfish docker/scripts scripts/
COPY docker/deployments/artemis-rar-${artemisVersion}.rar deployments/artemis.rar

CMD ./scripts/artemis-connect.sh && ./scripts/start-payara.sh
