HOST=$(hostname)
APP=${1}
if [ "${GC_LOG_ENABLED}" == "true" ]; then
  kcat -qP -t java-gc-log -H hostname="${HOST}" -H app="${APP}" -b "${BOOTSTRAP_SERVERS}" /tmp/gc.pipe &
  GC_LOG="-Xlog:gc*:file=/tmp/gc.pipe:time,uptime:filecount=0"
fi

java -Xms${INIT_HEAP:-128m} -Xmx${MAX_HEAP:-256m} -XX:+ExitOnOutOfMemoryError ${GC_LOG} -jar /work/${APP}
