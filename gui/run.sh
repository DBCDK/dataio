#!/bin/bash

docker run -e JAVA_MAX_HEAP_SIZE=2g \
-e JAVA_MAX_HEAP_SIZE="1G" \
-e SATURN_URL="http://dataio-saturn-service.metascrum-staging.svc.cloud.dbc.dk" \
-e TICKLE_REPO_HARVESTER_URL="http://dataio-tickle-harvester-service.metascrum-staging.svc.cloud.dbc.dk/dataio/harvester/tickle-repo" \
-e FILESTORE_URL="http://dataio-filestore-service.metascrum-staging.svc.cloud.dbc.dk/dataio/file-store-service" \
-e JOBSTORE_URL="http://dataio-jobstore-service.metascrum-staging.svc.cloud.dbc.dk/dataio/job-store-service" \
-e TZ="Europe/Copenhagen" \
-e SUBVERSION_URL="https://svn.dbc.dk/repos" \
-e PERIODIC_JOBS_HARVESTER_URL="http://dataio-periodic-jobs-harvester-service.metascrum-staging.svc.cloud.dbc.dk" \
-e FLOWSTORE_URL="http://dataio-flowstore-service.metascrum-staging.svc.cloud.dbc.dk/dataio/flow-store-service" \
-e FTP_URL="ftp://anonymous:info%40dbc.dk@ftp-staging.dbc.dk/datain" \
-e LOGSTORE_URL="http://dataio-logstore-service.metascrum-staging.svc.cloud.dbc.dk/dataio/log-store-service/" \
-e ELK_URL="http\\://elk.dbc.dk\:5601/#/discover/Traceid\:-Staging?_g\=(time\:(from\:now-7d,mode\:quick,to\:now))&_a\=(query\:(query_string\:(analyze_wildcard\:!t,query\:\\'%22@TRACEID@%22\\')),sort\:!(\\'@timestamp\\',asc))" \
-e LOG_FORMAT=text \
-p 8080:8080 docker-metascrum.artifacts.dbccloud.dk/dbc-payara-gui:devel
