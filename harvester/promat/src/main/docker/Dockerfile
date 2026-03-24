FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:${docker.payara.version}
LABEL org.opencontainers.image.authors="${team}"

ENV TZ=Europe/Copenhagen

ADD target/*.war target/docker/*.json deployments/
