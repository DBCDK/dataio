FROM docker-dbc.artifacts.dbccloud.dk/payara6-micro:${docker.payara.version}
LABEL org.opencontainers.image.authors="${team}"

ENV APP_NAME=${app-name} \
    TZ=Europe/Copenhagen

RUN printf '%s\n' \
  'set server.thread-pools.thread-pool.http-thread-pool.max-thread-pool-size=200' \
  'set server.thread-pools.thread-pool.http-thread-pool.min-thread-pool-size=50' \
  'set server.thread-pools.thread-pool.thread-pool-1.max-thread-pool-size=200' \
  'set server.thread-pools.thread-pool.thread-pool-1.min-thread-pool-size=50' \
  >> scripts/prebootcommandfile.txt

ADD target/*.war target/docker/*.json target/docker/hz-data.xml deployments/