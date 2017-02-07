#!/usr/bin/env bash
echo "==> Preparing for deploy of dataio-sink-oclc-1.0-SNAPSHOT.war"
cp $GLASSFISH_USER_HOME/*.war $GLASSFISH_HOME/glassfish/domains/domain1/autodeploy/