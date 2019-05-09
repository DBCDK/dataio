FROM docker-io.dbc.dk/dbc-payara-logback:latest

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

# Mechanism to select messages in queue based on name
ENV MESSAGE_NAME_FILTER dataio/sinks/ims/ws

ADD dbc-payara.d/* $PAYARA_USER_HOME/dbc-payara.d/

USER root


# Use confd to substitute
ADD confd /etc/confd

ADD *.war $PAYARA_USER_HOME

RUN chown -R $PAYARA_USER:$PAYARA_USER $PAYARA_USER_HOME && \
    mkdir -p /data/dataio && chown -R $PAYARA_USER:$PAYARA_USER /data/dataio
EXPOSE 4848

