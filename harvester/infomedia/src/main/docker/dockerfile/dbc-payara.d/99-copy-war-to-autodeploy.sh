#!/usr/bin/env bash
echo "==> Preparing for deploy of dataio-harvester-infomedia war"
cp $PAYARA_USER_HOME/*.war $PAYARA_HOME/glassfish/domains/domain1/autodeploy/