#!/usr/bin/env bash

# Stop on error
set -e

# when the shell expands, dont include the pattern in the list of results (e_g_ exclude *_xml)
shopt -s nullglob

WAR=dataio-sink-vip-1.0-SNAPSHOT


mkdir ${PAYARA_USER_HOME}/${WAR}

mv ${PAYARA_USER_HOME}/${WAR}.war ${PAYARA_USER_HOME}/${WAR}

cd ${PAYARA_USER_HOME}/${WAR}

unzip ${WAR}.war
rm ${WAR}.war

MODEL_FILE=${PAYARA_USER_HOME}/${WAR}/WEB-INF/ejb-jar.xml
cp ${PAYARA_USER_HOME}/dbc-payara.d/02-ejb-jar.xml $MODEL_FILE
rm ${PAYARA_USER_HOME}/dbc-payara.d/02-ejb-jar.xml

cd ${PAYARA_USER_HOME}/${WAR}

jar cmf META-INF/MANIFEST.MF ${WAR}.war *
mv ${WAR}.war ..
cd ..

rm -rf ${WAR}

