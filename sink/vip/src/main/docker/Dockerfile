FROM docker-dbc.artifacts.dbccloud.dk/payara5-full:old-202246

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

COPY dataio-sink-vip-${project.version}.war docker/sink-vip.json deployments/

