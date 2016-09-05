# dbc-glassfish-jobprocessor

FROM docker-io.dbc.dk/dbc-glassfish
ARG build_number=unknown
LABEL BuildNumber=$build_number

MAINTAINER dataio

ENV LOGBACK_URL https://svn.dbc.dk/repos/dataio/trunk/deploy/config/jobprocessor-logback-prod-docker.xml

ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/
ADD confd /etc/confd


ADD *.war $GLASSFISH_USER_HOME

USER root

RUN chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_USER_HOME
EXPOSE 4848


