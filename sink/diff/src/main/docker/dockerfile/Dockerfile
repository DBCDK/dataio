# dbc-payara-diff-sink

FROM docker-io.dbc.dk/dbc-payara-logback:stretch

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG git_commit=unknown
LABEL GitCommit=$git_commit

ADD dbc-payara.d/* $PAYARA_USER_HOME/dbc-payara.d/

ADD script/* /bin/

USER root

RUN apt-get install -qy diffutils colordiff sed libxml2-utils jq

# Use confd to substitute
ADD confd /etc/confd

ADD *.war $PAYARA_USER_HOME

RUN chown -R $PAYARA_USER:$PAYARA_USER $PAYARA_USER_HOME && \
    mkdir -p /data/dataio && chown -R $PAYARA_USER:$PAYARA_USER /data/dataio
RUN chmod +x /bin/xmldiff
EXPOSE 4848
