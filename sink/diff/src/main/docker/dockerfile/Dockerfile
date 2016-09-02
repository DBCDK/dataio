FROM docker-io.dbc.dk/dbc-glassfish

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number

# Mechanism to select messages in queue based on name
ENV MESSAGE_NAME_FILTER jdbc/dataio/sinks/diff/test3
ENV QUEUE_PORT 7676

ENV LOGBACK_URL https://svn.dbc.dk/repos/dataio/trunk/deploy/config/diff-logback-docker-stg.xml

ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/

USER root

# Use confd to substitute
ADD confd /etc/confd

ADD *.war $GLASSFISH_USER_HOME

RUN chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_USER_HOME && \
    mkdir -p /data/dataio && chown -R $GLASSFISH_USER:$GLASSFISH_USER /data/dataio
EXPOSE 4848

