FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit
ARG artemisVersion=2.23.0
COPY --chown=gfish docker/scripts scripts/
# REMOVE
COPY docker/deployments/payara-artemis.json deployments/payara-artemis.json
# END REMOVE
COPY docker/deployments/artemis-rar-2.23.0.rar deployments/artemis.rar

CMD ./scripts/artemis-connect.sh && ./scripts/start-payara.sh
