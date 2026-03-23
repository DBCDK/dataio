FROM docker-dbc.artifacts.dbccloud.dk/dbc-java17:latest
LABEL org.opencontainers.image.authors="de-team"

ENV TZ=Europe/Copenhagen

COPY --chown=java:java target/${prometheus.jar} /home/java/app/
COPY --chown=java:java target/docker/config.yaml /home/java/app/

USER java
WORKDIR /home/java/app
CMD ["/bin/sh", "-c", "/bin/bash -c \"envsubst < config.yaml > config-x.yaml && java -jar -Xmx256m ${prometheus.jar} 8080 config-x.yaml\""]