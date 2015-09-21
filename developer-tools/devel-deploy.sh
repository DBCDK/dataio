#!/usr/bin/env bash
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

function die() {
  echo ERROR : $*
  exit 1
}

VERSION="1.0-SNAPSHOT"
COMPONENTS="dataio-file-store-service-ear dataio-flow-store-service dataio-job-store-service-war dataio-log-store-service-war dataio-sink-service"
#COMPONENTS="dataio-flow-store-service"

APPS=""

for c in $COMPONENTS ; do
   path=$(find . -type f -name ${c}-${VERSION}.?ar) || die "unable to find . -type f -name ${c}-${VERSION}.?ar"
   if [ "x$path" == "x" ] ; then die "unable to locate component $c" ; fi
   APPS="$APPS $path"
done

echo "apps = $APPS"

ASADMIN=`pwd`/integration-test/glassfish/home/glassfish4/bin/asadmin
ASADMIN_CMD="$ASADMIN --port 4848 --user admin"


for app in $APPS ; do
  echo "deploying $app"
  $ASADMIN_CMD deploy --force=true $app
done