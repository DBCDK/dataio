FROM docker-dbc.artifacts.dbccloud.dk/dbc-java11:master

ARG GATEKEEPER_JAR=dataio-gatekeeper-*.jar

COPY ${GATEKEEPER_JAR} dataio-gatekeeper.jar
ARG FTP_HOME=/data/ftp
RUN apt-get update -y
RUN apt-install proftpd -y
COPY etc /etc/
RUN mkdir -p ${FTP_HOME}/datain && chown -R ftp ${FTP_HOME} && usermod -d ${FTP_HOME} ftp

CMD bash -xc 'proftpd && \
                java -Dcom.sun.management.jmxremote.port=1868 \
                    -Dcom.sun.management.jmxremote.ssl=false \
                    -Dcom.sun.management.jmxremote.authenticate=false \
                    -Xmx256m \
                    -jar dataio-gatekeeper.jar \
                    -d "/data/ftp/datain" \
                    -f "${FILESTORE_SVC:-http://dataio-filestore-service.metascrum-staging.svc.cloud.dbc.dk/dataio/file-store-service}" \
                    -j "${JOBSTORE_SVC:-http://dataio-jobstore-service.metascrum-staging.svc.cloud.dbc.dk/dataio/job-store-service}" \
                    -c "${FLOWSTORE_SVC:-http://dataio-flowstore-service.metascrum-staging.svc.dbc.dk/dataio/flow-store-service}"'
