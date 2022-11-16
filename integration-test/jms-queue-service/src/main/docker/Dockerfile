FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:old-202246

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-integration-test-jms-queue-service-${project.version}.war docker/jms-queue-service.json deployments/
