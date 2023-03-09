#!/bin/bash -e
FTP_HOME=/data/ftp
ISWORKER_HOME=/home/isworker
IS_SSH=${ISWORKER_HOME}/.ssh
GKEEP_HOME=/home/gkeep
GKEEP_BIN=${GKEEP_HOME}/bin

apt-install proftpd -y
mkdir -p ${FTP_HOME}/datain
useradd -mr -s /bin/bash -d ${FTP_HOME} ftp
useradd -mr -s /bin/bash -d ${GKEEP_HOME} gkeep
useradd -mr -s /bin/bash -d ${ISWORKER_HOME} isworker
chown -R ftp ${FTP_HOME}
mkdir ${GKEEP_BIN}
chown -R isworker:is:worker ${GKEEP_BIN}
mkdir ${IS_SSH}
chown -R isworker:is:worker ${IS_SSH}
chmod -R go-rwx ${IS_SSH}
systemctl daemon-reload
systemctl enable gatekeeper.service
