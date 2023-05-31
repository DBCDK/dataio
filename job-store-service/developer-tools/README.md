JOBSTORE DEVELOPER TOOLS
========================
This is a simple setup of jobstore/queues/consumers aimed at debugging jobstore.
Requires:
* jq
* docker compose

No actual data processing of records takes place. Processor and sink will just nod and say yes to any kind of record data.

Fix the ``src/main/resources/docker-compose/compose.yml`` so that it reflects the jobstore version you want to debug.

Then:
```bash
docker compose -f ./src/main/resources/docker-compose/compose.yml up 
```

You can create jobs using a "developer-mode-only" endpoint:
```bash
curl -X POST -d @src/main/resources/testdata/dmat.json -H "Content-Type: application/json" \ 
localhost:<whatever-port-jobstore-is-at>/dataio/job-store-service/jobs/developer/ADDI_MARC_XML 
```
(You might take note of the jobstore exposed http port with ``docker ps`` prior to this)

Last part of the path is the record splitter (refer to class RecordSplitterConstants in dataio.commons).
There are two examples of JobInputStream to use. You have to dig a little in recent in dataio-gui jobs to find a suitable file in filestore to use. 

To start consuming from the jobprocessor queue:
```bash
src/main/scripts/start-queue-eater "processor::business" PROCESSED 8081 
```

To start consuming from the sinks queue:
```bash
src/main/scripts/start-queue-eater "sinkqueue1::sinkqueue1" DELIVERED 8082 
```
