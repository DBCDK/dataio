# dbc-glassfish-update-sink

FROM docker-io.dbc.dk/dbc-glassfish

MAINTAINER dataio
ARG build_number=unknown
LABEL BuildNumber=$build_number

# UPDATEJNDI is a naming convention used when filtering messages from a queue.
#   I think the queue name is hardcoded, and the filtering is done on the message names,
#   and that UPDATEJNDI is that message name.
ENV PARAMS_UPDATEJNDI url/dataio/openupdate/ws

ENV WAR dataio-sink-openupdate-1.0-SNAPSHOT
ENV LOGBACK_URL https://svn.dbc.dk/repos/dataio/trunk/deploy/config/logback-docker-generic.xml


ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/

USER root

# Use confd
ADD confd /etc/confd

ADD *.war $GLASSFISH_USER_HOME

USER root

RUN chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_USER_HOME && \
    mkdir -p /data/dataio && chown -R $GLASSFISH_USER:$GLASSFISH_USER /data/dataio
EXPOSE 4848 8080