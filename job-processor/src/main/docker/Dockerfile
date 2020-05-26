FROM docker.dbc.dk/payara5-full:java11-6

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY docker/dataio-log-store-service-logback-appender-${project.version}.jar \
     docker/dataio-log-store-service-types-${project.version}.jar \
     docker/invariant-utils-${invariant-utils.version}.jar \
     docker/postgresql-42.0.0.jar \
     glassfish/dbc/

COPY docker/logback.xml scripts/
COPY dataio-job-processor-${project.version}.war docker/job-processor.json deployments/