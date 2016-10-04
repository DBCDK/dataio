#start three acctests on dataio prod for FFU prepare flow

../create_job.py --jobstorehost jobstore.dataio.prod.mcp1.dbc.dk --filestorehost dataio-be-p01:8080 820010.FFU-acctest-records.iso 820010.ACCTEST.FFU-prepare.PROD.spec
sleep 2
../create_job.py --jobstorehost jobstore.dataio.prod.mcp1.dbc.dk --filestorehost dataio-be-p01:8080 810015.FFU-acctest-records.iso 810015.ACCTEST.FFU-prepare.PROD.spec
sleep 2
../create_job.py --jobstorehost jobstore.dataio.prod.mcp1.dbc.dk --filestorehost dataio-be-p01:8080 850330.FFU-acctest-records.iso 850330.ACCTEST.FFU-prepare.PROD.spec


