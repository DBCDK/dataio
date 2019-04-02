# dbc-payara-harvester

FROM docker-io.dbc.dk/dbc-payara-logback:stretch

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

ADD dbc-payara.d/* $PAYARA_USER_HOME/dbc-payara.d/

USER root

# Use confd to substitute
ADD confd /etc/confd

ADD *.war $PAYARA_USER_HOME

RUN apt-get -o Acquire::Check-Valid-Until=false update &&  apt-get install -y python

RUN chown -R $PAYARA_USER:$PAYARA_USER $PAYARA_USER_HOME && \
    mkdir -p /data/dataio && chown -R $PAYARA_USER:$PAYARA_USER /data/dataio
EXPOSE 4848
