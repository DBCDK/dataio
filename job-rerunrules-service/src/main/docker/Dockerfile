FROM docker.dbc.dk/payara-micro

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

ENV PROJECT_VERSION 1.0-SNAPSHOT
ENV USER gfish
USER $USER

COPY dataio-job-rerunrules-service-$PROJECT_VERSION.war wars

EXPOSE 4848 8080
