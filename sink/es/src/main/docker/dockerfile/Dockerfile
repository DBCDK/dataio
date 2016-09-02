FROM docker-io.dbc.dk/dbc-glassfish

MAINTAINER dataio
ARG build_number=unknown
LABEL BuildNumber=$build_number

ENV DBC_ES_ENV_USER test_brond_upd
ENV DBC_ES_ENV_PASSWORD test
ENV DBC_ES_ENV_TNSNAME tora1.dbc.dk
ENV DBC_ES_ENV_PORT 1521
ENV DBC_ES_ENV_DB danbib_int_base
ENV ES_USER_ID 0
ENV SINK_ID -1

ENV ES_DATABASENAME danbib_int_base

# Mechanism to select messages in queue based on name
ENV MESSAGE_NAME_FILTER jdbc/dataio/sinks/es/test3
ENV QUEUE_PORT 7676

ENV LOGBACK_URL https://svn.dbc.dk/repos/dataio/trunk/deploy/config/es-logback-docker-stg.xml

ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/

USER root

# Use confd to substitute
ADD confd /etc/confd

ADD *.war $GLASSFISH_USER_HOME

RUN chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_USER_HOME && \
    mkdir -p /data/dataio && chown -R $GLASSFISH_USER:$GLASSFISH_USER /data/dataio
EXPOSE 4848

