FROM docker.dbc.dk/payara5-micro:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

RUN echo "set server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=500" >>scripts/prebootcommandfile.txt

COPY dataio-flow-store-service-${project.version}.war docker/flowstore.json deployments/