#!/usr/bin/env bash

source /opt/payara6/scripts/common-log.bash

function terminate_payara_gracefully() {
  info "entering payara($payara_pid) graceful shutdown trap"
  # mandatory 10s grace period to account for asynchronicity in
  # kubernetes POD termination
  sleep 10

  # wait for no active TCP connections before sending SIGTERM
  while :
  do
    STAT=$(netstat -t | grep ':8080 ' | grep ESTABLISHED)
    active=$(echo ${STAT} | awk '{print $4}' | wc -l)
    if [ "$active" == "0" ]; then
      info "no active connections found"
      break;
    fi
    info "found $active connections"
    echo ${STAT}
    sleep 1
  done

  info "sending SIGTERM to payara"
  kill -SIGTERM "$payara_pid"
}

trap terminate_payara_gracefully SIGTERM

function interrupt_payara() {
  info "entering payara($payara_pid) interrupt shutdown trap"
  info "sending SIGTERM to payara"
  kill -SIGTERM "$payara_pid"
}

trap interrupt_payara SIGINT

export logbackDisableServletContainerInitializer=true

if [ -z "$JAVA_MAX_HEAP_SIZE" ] ; then
  die "JAVA_MAX_HEAP_SIZE must be set to java max heap size"
fi

export _PAYARA_MAX_HEAP=0
case $JAVA_MAX_HEAP_SIZE in
    *[mM])
	    _PAYARA_MAX_HEAP=$(( ${JAVA_MAX_HEAP_SIZE%[mM]} ))m
	    ;;
	*[gG])
		_PAYARA_MAX_HEAP=$(( ${JAVA_MAX_HEAP_SIZE%[gG]} * 1024))m
	    ;;
	*)
	die "JAVA_MAX_HEAP_SIZE must be in M or G bytes. JAVA_MAX_HEAP_SIZE was $JAVA_MAX_HEAP_SIZE"
	;;
esac

if [ ${_PAYARA_MAX_HEAP%m} -lt 1024 ] ; then
   die "JAVA_MAX_HEAP_SIZE must be greater than or equal to 1g, JAVA_MAX_HEAP_SIZE was $JAVA_MAX_HEAP_SIZE"
fi

JAVA_EXTRA_OPTIONS_FILE=/opt/payara6/scripts/jdk_options_file.txt
# Lest payara warns about private field jdk.internal.loader.URLClassPath.loaders not being accessible
echo "--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED" > ${JAVA_EXTRA_OPTIONS_FILE}
# Use additional Java arguments to provide Hazelcast access to Java internal API.
# The internal API access is used to get the best performance results.
echo "--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.net=ALL-UNNAMED --add-opens java.base/sun.net.www.protocol.jar=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED" >> ${JAVA_EXTRA_OPTIONS_FILE}
function get_jvm_extra_options() {
  cat ${JAVA_EXTRA_OPTIONS_FILE} | tr '\n' ' '
}

java -jar ./scripts/payara-configreader.jar --payara-kind=${PAYARA_KIND} --payara-config-dir=${PAYARA_CONFIG_DIR} --prebootcommandfile ${PRE_BOOT_COMMAND_FILE} --postbootcommandfile ${POST_BOOT_COMMAND_FILE} --jvm-extra-options-file ${JAVA_EXTRA_OPTIONS_FILE} ${DEPLOY_DIR}/*.json || die "Errors from payara-configreader - probably caused by error in the json file"

k8s-dns-wait
