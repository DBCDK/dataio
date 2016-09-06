# dbc-glassfish-harvester

FROM docker-io.dbc.dk/dbc-glassfish

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number

ENV LOGBACK_URL https://svn.dbc.dk/repos/dataio/trunk/deploy/config/logback-docker-generic.xml
ENV URL_DATAIO_USH_HARVESTER not_specified
# For staging er det det her: http://dataio-ush-s01.dbc.dk:8080/harvester

ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/

# Use confd to substitute
ADD confd /etc/confd

ADD *.war $GLASSFISH_USER_HOME

USER root
RUN apt-get update &&  apt-get install -y python

RUN chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_USER_HOME && \
    mkdir -p /data/dataio && chown -R $GLASSFISH_USER:$GLASSFISH_USER /data/dataio
