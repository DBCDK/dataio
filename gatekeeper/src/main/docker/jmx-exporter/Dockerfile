FROM docker-dbc.artifacts.dbccloud.dk/dbc-java11:master
MAINTAINER Jens Grønborg Andersen <jega@dbc.dk>
ARG JMX_MIGRATOR_VERSION
RUN useradd -mr jmx -s /bin/bash
USER jmx
WORKDIR /home/jmx
COPY --chown=jmx jmx_prometheus_httpserver-*.jar jmx_prometheus_httpserver.jar
COPY --chown=jmx config.yaml config.yaml
EXPOSE 8080/tcp
ENV JMX_HOST=localhost:1868
CMD /bin/bash -c "envsubst < config.yaml > config-x.yaml && java -jar -Xmx256m jmx_prometheus_httpserver.jar 8080 config-x.yaml"
