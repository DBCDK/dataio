FROM docker.dbc.dk/payara5-full:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY docker/dataio-log-store-service-logback-appender-${project.version}.jar \
     docker/dataio-log-store-service-types-${project.version}.jar \
     docker/invariant-utils-${invariant-utils.version}.jar \
     docker/postgresql-${postgresql.driver.version}.jar \
     glassfish/dbc/

COPY docker/logback.xml scripts/
COPY dataio-job-processor-${project.version}.war docker/job-processor.json deployments/
