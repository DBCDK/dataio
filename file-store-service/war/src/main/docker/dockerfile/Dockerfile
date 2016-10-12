FROM docker-io.dbc.dk/dbc-glassfish

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG svn_revision=unknown
LABEL SvnRevision=$svn_revision


USER root

ADD *.war $GLASSFISH_USER_HOME

ADD dbc-glassfish.d/* $GLASSFISH_USER_HOME/dbc-glassfish.d/

RUN 	mkdir -p /data/dataio/filestore && \
	chown -R $GLASSFISH_USER:$GLASSFISH_USER /data/dataio && \
	chown -R $GLASSFISH_USER:$GLASSFISH_USER $GLASSFISH_HOME

# Use confd to substitute
ADD confd /etc/confd

WORKDIR $GLASSFISH_USER_HOME

EXPOSE 4848