#!/bin/bash
docker run \
-e JAVA_MAX_HEAP_SIZE=2g \
-e ARTEMIS_MQ_HOST="172.17.28.82" \
-e MESSAGE_NAME_FILTER="tickle-repo/total" \
-e JOBSTORE_URL="http://dataio-jobstore-service.metascrum-staging.svc.cloud.dbc.dk/dataio/job-store-service" \
-e TICKLE_REPO_DB_URL="dataio_tickle:hQMeepvNys9E@db.dataio-tickle-v14.stg.dbc.dk:5432/dataio_tickle_db" \
-e TICKLE_BEHAVIOUR=total \
-e REMOTE_DEBUGGING_HOST="172.17.28.82:5005" \
 docker-metascrum.artifacts.dbccloud.dk/dbc-payara-tickle-repo-sink:devel
