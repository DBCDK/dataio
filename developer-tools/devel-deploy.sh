#!/usr/bin/env bash

function die() {
  echo ERROR : $*
  exit 1
}

VERSION="1.0-SNAPSHOT"
COMPONENTS="dataio-file-store-service-ear dataio-flow-store-service dataio-new-job-store-service-war dataio-log-store-service-ear dataio-job-store-service-ear dataio-sink-service"
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
