FROM docker-io.dbc.dk/dbc-payara-logback:latest

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit


USER root

ADD *.war $PAYARA_USER_HOME

ADD dbc-payara.d/* $PAYARA_USER_HOME/dbc-payara.d/

RUN 	mkdir -p /data/dataio/filestore && \
	chown -R $PAYARA_USER:$PAYARA_USER /data/dataio && \
	chown -R $PAYARA_USER:$PAYARA_USER $PAYARA_HOME

# Use confd to substitute
ADD confd /etc/confd

WORKDIR $PAYARA_USER_HOME

EXPOSE 4848