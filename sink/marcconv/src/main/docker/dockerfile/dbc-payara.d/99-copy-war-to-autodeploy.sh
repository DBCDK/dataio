#!/usr/bin/env bash
echo "==> Preparing for deploy of dataio-sink-marcconv-1.0-SNAPSHOT.war"
cp $PAYARA_USER_HOME/*.war $PAYARA_HOME/glassfish/domains/domain1/autodeploy/