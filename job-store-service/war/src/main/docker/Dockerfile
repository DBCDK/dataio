FROM docker.dbc.dk/payara5-full:latest

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

RUN echo "set server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=200\n\
set server.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size=50\n\
set server.thread-pools.thread-pool.thread-pool-1.max-thread-pool-size=200\n\
set server.thread-pools.thread-pool.thread-pool-1.min-thread-pool-size=50" >>scripts/prebootcommandfile.txt

COPY dataio-job-store-service-war-${project.version}.war docker/job-store.json deployments/