# dbc-glassfish-update-sink

FROM docker-io.dbc.dk/dbc-glassfish

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG svn_revision=unknown
LABEL SvnRevision=$svn_revision

ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/

USER root

# Use confd
ADD confd /etc/confd

ADD *.war $GLASSFISH_USER_HOME

USER root

RUN chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_USER_HOME && \
    mkdir -p /data/dataio && chown -R $GLASSFISH_USER:$GLASSFISH_USER /data/dataio
EXPOSE 4848 8080
