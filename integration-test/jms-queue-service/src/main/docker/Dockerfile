FROM docker-metascrum.artifacts.dbccloud.dk/dbc-payara-artemis-base:devel
ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-integration-test-jms-queue-service-${project.version}.war docker/jms-queue-service.json deployments/
