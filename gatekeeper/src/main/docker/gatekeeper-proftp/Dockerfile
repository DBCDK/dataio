FROM docker-dbc.artifacts.dbccloud.dk/dbc-java11:master

ARG GATEKEEPER_JAR=dataio-gatekeeper-*.jar

ARG FTP_HOME=/data/ftp
RUN apt-get update -y
RUN apt-install proftpd -y
RUN apt-install tcpdump -y
COPY etc /etc/
RUN mkdir -p ${FTP_HOME}/datain && chown -R ftp ${FTP_HOME} && usermod -d ${FTP_HOME} ftp
RUN useradd -mr -s /bin/bash -d /home/gkeep gkeep
WORKDIR /home/gkeep
COPY gatekeeper.sh gatekeeper.sh
COPY ${GATEKEEPER_JAR} dataio-gatekeeper.jar
CMD bash -xc 'proftpd && su - gkeep -c ./gatekeeper.sh'
