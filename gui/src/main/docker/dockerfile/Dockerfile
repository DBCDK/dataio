#
# DataIO - Data IO
# Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
# Denmark. CVR: 15149043
#
# This file is part of DataIO.
#
# DataIO is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# DataIO is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
#

FROM docker-i.dbc.dk/dbc-payara:latest

MAINTAINER dataio

ARG build_number=unknown
LABEL BuildNumber=$build_number
ARG svn_revision=unknown
LABEL SvnRevision=$svn_revision
ENV SVN_REVISION $svn_revision
ADD dbc-payara.d/* $PAYARA_USER_HOME/dbc-payara.d/

USER root

# Use confd to substitute
ADD confd /etc/confd

ADD *.war $PAYARA_USER_HOME

RUN chown -R $PAYARA_USER:$PAYARA_USER $PAYARA_USER_HOME && \
    mkdir -p /data/dataio && chown -R $PAYARA_USER:$PAYARA_USER /data/dataio
EXPOSE 4848

