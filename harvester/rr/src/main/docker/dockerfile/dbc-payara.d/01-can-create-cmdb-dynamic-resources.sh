#!/usr/bin/env bash

set -e
echo env
echo $RAWREPOS | python $PAYARA_USER_HOME/dbc-payara.d/generateExtraRRPools.py $PAYARA_USER_HOME/dbc-payara.d/65-extra-rr.xml