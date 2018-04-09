#!/usr/bin/env bash

# Stop on error
set -e

# when the shell expands, dont include the pattern in the list of results (e_g_ exclude *_xml)
shopt -s nullglob

WAR=dataio-job-processor-1.0-SNAPSHOT


mkdir ${PAYARA_USER_HOME}/${WAR}

mv ${PAYARA_USER_HOME}/${WAR}.war ${PAYARA_USER_HOME}/${WAR}

cd ${PAYARA_USER_HOME}/${WAR}

unzip ${WAR}.war
rm ${WAR}.war

MODEL_FILE=${PAYARA_USER_HOME}/${WAR}/WEB-INF/ejb-jar.xml
cp ${PAYARA_USER_HOME}/dbc-payara.d/02-ejb-jar.xml ${MODEL_FILE}

# Copy postgres logstore specific logback dependencies to top payara endorsed dir
cp ${PAYARA_USER_HOME}/${WAR}/WEB-INF/lib/dataio-log-store-service-logback-appender-1.0-SNAPSHOT.jar ${PAYARA_HOME}/glassfish/lib/endorsed
cp ${PAYARA_USER_HOME}/${WAR}/WEB-INF/lib/dataio-log-store-service-types-1.0-SNAPSHOT.jar ${PAYARA_HOME}/glassfish/lib/endorsed
cp ${PAYARA_USER_HOME}/${WAR}/WEB-INF/lib/invariant-utils-0.1.0.jar ${PAYARA_HOME}/glassfish/lib/endorsed

rm ${PAYARA_USER_HOME}/dbc-payara.d/02-ejb-jar.xml

cd ${PAYARA_USER_HOME}/${WAR}

jar cmf META-INF/MANIFEST.MF ${WAR}.war *
mv ${WAR}.war ..
cd ..

rm -rf ${WAR}
