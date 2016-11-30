#!/usr/bin/env bash

# Stop on error
set -e

# when the shell expands, dont include the pattern in the list of results (e_g_ exclude *_xml)
shopt -s nullglob

WAR=dataio-sink-batch-exchange-1.0-SNAPSHOT


mkdir ${GLASSFISH_USER_HOME}/${WAR}

mv ${GLASSFISH_USER_HOME}/${WAR}.war ${GLASSFISH_USER_HOME}/${WAR}

cd ${GLASSFISH_USER_HOME}/${WAR}

unzip ${WAR}.war
rm ${WAR}.war

MODEL_FILE=${GLASSFISH_USER_HOME}/${WAR}/WEB-INF/ejb-jar.xml
cp ${GLASSFISH_USER_HOME}/dbc-glassfish.d/02-ejb-jar.xml $MODEL_FILE
rm ${GLASSFISH_USER_HOME}/dbc-glassfish.d/02-ejb-jar.xml

cd ${GLASSFISH_USER_HOME}/${WAR}

jar cmf META-INF/MANIFEST.MF ${WAR}.war *
mv ${WAR}.war ..
cd ..

rm -rf ${WAR}

