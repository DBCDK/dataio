FROM docker-dbc.artifacts.dbccloud.dk/payara7-micro:${docker.payara.version}
LABEL org.opencontainers.image.authors="${team}"

ENV APP_NAME=${app-name} \
    TZ=Europe/Copenhagen

ADD target/*.war target/docker/*.json deployments/
