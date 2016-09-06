# dbc-glassfish-harvester

FROM docker-io.dbc.dk/dbc-glassfish

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number

ENV LOGBACK_URL https://svn.dbc.dk/repos/dataio/trunk/deploy/config/rr-harvester-docker-logback.xml

ENV DBC_POSTGRES_HARVESTER_ENV_POSTGRES_DB harvester_rr_db
ENV DBC_POSTGRES_HARVESTER_ENV_POSTGRES_PASSWORD harvester_rr
ENV DBC_POSTGRES_HARVESTER_ENV_POSTGRES_USER harvester_rr

ENV DBC_POSTGRES_HARVESTER_XA_ENV_POSTGRES_USER harvester_rr_xa
ENV DBC_POSTGRES_HARVESTER_XA_ENV_POSTGRES_PASSWORD harvester_rr_xa
ENV DBC_POSTGRES_HARVESTER_XA_ENV_POSTGRES_DB harvester_rr_xa_db

ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/

USER root

# Use confd to substitute
ADD confd /etc/confd

ADD *.war $GLASSFISH_USER_HOME

USER root
RUN apt-get update &&  apt-get install -y python

RUN chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_USER_HOME && \
    mkdir -p /data/dataio && chown -R $GLASSFISH_USER:$GLASSFISH_USER /data/dataio
EXPOSE 4848