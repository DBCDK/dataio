#!/usr/bin/env bash

set -e

#shopt -s nullglob
echo env
echo $RAWREPOS | python $GLASSFISH_USER_HOME/dbc-glassfish.d/generateExtraRRPools.py $GLASSFISH_USER_HOME/dbc-glassfish.d/65-extra-rr.xml