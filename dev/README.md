# DataIO Local Developer Stack

Runs the full processing pipeline locally for manual testing:

| Port | Service |
|------|---------|
| 8161 | Artemis admin console (browse queues) |
| 8080 | job-store REST API |
| 9009 | job-store remote debug |
| 8081 | flow-store REST API |
| 8082 | file-store REST API |
| 8083 | job-processor2 (Nashorn) health + metrics |
| 8084 | job-processor-graaljs health + metrics |
| 8085 | log-store REST API |

---

## Prerequisites

- Docker + Docker Compose v2 (`docker compose version`)
- Maven 3.9+
- Python 3.9+ (no third-party packages needed)
- `jq` and `zip`
- Access to `docker-metascrum.artifacts.dbccloud.dk` and `docker-dbc.artifacts.dbccloud.dk`

---

## 1. Build

Build Docker images (skip integration tests for speed):

```bash
mvn -pl flow-store-service,file-store-service,log-store-service/war,job-store-service/war,job-processor2/app,job-processor-graaljs \
    -am install -DskipITs
```

---

## 2. Start the stack

```bash
docker compose -f dev/docker-compose.yml up -d
```

Watch startup:

```bash
docker compose -f dev/docker-compose.yml logs -f
```

Wait until job-store is ready:

```bash
until curl -fs http://localhost:8080/dataio/job-store-service/status > /dev/null; do
  echo "waiting for job-store..."; sleep 5
done
echo "job-store ready"
```

---

## 3. Seed the flow-store (one-time per fresh stack)

The JSARs are pre-built and checked in to `dev/testdata/`. Run the seed script from the **project root**:

```bash
bash dev/scripts/seed-flowstore.sh
```

The script uploads both passthrough flows, creates the submitter and sink, and creates a flow binder for each processor engine. It is idempotent — re-running it on an already-seeded stack detects existing entities and skips them.

---

## 4. Submit jobs

### 4a. With the create-job script

`dev/scripts/create-job.py` uploads a data file to the file-store and submits a
job referencing it. Its built-in job specification matches the flow binders
created in step 3, so against the local stack a bare invocation is enough. Run
it from the project root. It uses the standard library only, so there is nothing
to install.

```bash
NASHORN_JOB_ID=$(python3 dev/scripts/create-job.py dev/testdata/sample-records.ndjson)
GRAALJS_JOB_ID=$(python3 dev/scripts/create-job.py dev/testdata/sample-records.ndjson --destination dev-graaljs)

echo "Nashorn job ID: $NASHORN_JOB_ID"
echo "GraalJS job ID: $GRAALJS_JOB_ID"
```

The job ID goes to stdout and progress goes to stderr, so the command
substitution above captures the ID alone.

#### Flow binder resolution

The job-store picks a flow binder by matching five fields of the job
specification against the binders registered in the flow-store: `packaging`,
`format`, `charset`, `submitterId` and `destination`. Each has its own flag, and
each takes any value the target flow-store knows about.

```bash
python3 dev/scripts/create-job.py data.addi \
  --packaging addi-xml --format dmat --charset utf8 \
  --submitter 150015 --destination dmat
```

`dev-nashorn` and `dev-graaljs` are only what `seed-flowstore.sh` happens to
register locally. A job whose five fields match no binder is created and then
fails with a `Could not retrieve FlowBinder` diagnostic, visible in the job
state (step 5).

Pass a whole specification with `--spec FILE` when you have one. It expects the
bare specification, not a job input stream wrapping it in a `jobSpecification`
field. Individual flags still override single fields on top of the file.

#### Other options

| Option | Effect |
|---|---|
| `--type` | Job type: `TRANSIENT`, `PERSISTENT`, `TEST` and the rest of `JobSpecification.Type`. |
| `--result-mail-initials` | Sets `resultmailInitials`. Optional, as are the notification mail fields. |
| `--datafile-id ID` | Reuses a file already in the file-store instead of uploading a new one. |
| `--dry-run` | Prints the job input stream that would be posted and stops, without touching the network. |
| `--part-number N`, `--not-end-of-job` | Submit a job in several parts. |
| `--timeout SECONDS` | Per-request timeout, 60 by default. |

`--help` lists them all.

#### Submitting to staging or production

`--instance` picks the target. `local` is the default. `staging` and `prod` each
carry the cluster URLs of the two services the script talks to:

| Instance | file-store | job-store |
|---|---|---|
| `local` | `localhost:8082` | `localhost:8080` |
| `staging` | `dataio-filestore-service.metascrum-staging.svc.cloud.dbc.dk` | `dataio-jobstore-service.metascrum-staging.svc.cloud.dbc.dk` |
| `prod` | `dataio-filestore-service.metascrum-prod.svc.cloud.dbc.dk` | `dataio-jobstore-service.metascrum-prod.svc.cloud.dbc.dk` |

Each is reached over plain HTTP at the usual `/dataio/<service>-service` path.
`--dry-run` prints the pair a name stands for.

```bash
python3 dev/scripts/create-job.py records.xml --instance staging --spec myjob.json
```

Two things differ from a local run:

- The built-in job specification is refused, since it only resolves against the
  seeded local binders. Pass `--spec`, or give `--type` and all five resolution
  fields as flags.
- The script prints the target and asks for confirmation before submitting.
  `--yes` skips the prompt, and is required when stdin is not a terminal.

`--file-store URL` and `--job-store URL` address the two services directly and
take precedence over the instance. Naming a non-local service URL counts as a
non-local target, prompt and all.

`--instance` also accepts the URL of an instance that serves a `/urls` map, such
as `http://dataio.dbc.dk`, in which case the two service URLs are read from
there. That is how `cli/job-replicator` and `cli/datafile-exporter` find their
endpoints.

### 4b. By hand with curl

Upload the data file and keep the returned file-store ID:

```bash
FILE_URN=$(curl -s -D - -X POST http://localhost:8082/dataio/file-store-service/files \
  -H "Content-Type: application/octet-stream" \
  --data-binary @dev/testdata/sample-records.ndjson \
  | grep -i "^location:" | sed 's|.*/files/||' | tr -d '[:space:]')

echo "File URN: urn:dataio-fs:$FILE_URN"
```

Submit to Nashorn:

```bash
JOB=$(curl -s -X POST \
  http://localhost:8080/dataio/job-store-service/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobSpecification": {
      "type": "TRANSIENT",
      "format": "test",
      "charset": "utf8",
      "dataFile": "urn:dataio-fs:'"$FILE_URN"'",
      "packaging": "JSON",
      "destination": "dev-nashorn",
      "submitterId": 870970,
      "resultmailInitials": "DEV"
    },
    "isEndOfJob": true,
    "partNumber": 0
  }')

NASHORN_JOB_ID=$(echo "$JOB" | jq '.jobId')
echo "Nashorn job ID: $NASHORN_JOB_ID"
```

Submit to GraalJS, which differs only in the destination:

```bash
JOB=$(curl -s -X POST \
  http://localhost:8080/dataio/job-store-service/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobSpecification": {
      "type": "TRANSIENT",
      "format": "test",
      "charset": "utf8",
      "dataFile": "urn:dataio-fs:'"$FILE_URN"'",
      "packaging": "JSON",
      "destination": "dev-graaljs",
      "submitterId": 870970,
      "resultmailInitials": "DEV"
    },
    "isEndOfJob": true,
    "partNumber": 0
  }')

GRAALJS_JOB_ID=$(echo "$JOB" | jq '.jobId')
echo "GraalJS job ID: $GRAALJS_JOB_ID"
```

The job-store requires `Content-Type: application/json` on this call and answers
415 without it.

---

## 5. Monitor status

### Job state

The job-store has no `GET /jobs/{id}` endpoint. Use `GET /jobs/queries?q=` with an IOQL expression for parameterised lookups, or `POST /jobs/searches` with a filter object for open-ended queries:

```bash
# Single job by ID
curl -s "http://localhost:8080/dataio/job-store-service/jobs/queries?q=job:id+%3D+$NASHORN_JOB_ID" \
  | jq '.[0] | {jobId, partitioning: .state.states.PARTITIONING, processing: .state.states.PROCESSING, delivering: .state.states.DELIVERING}'

# All jobs (compact) — empty filtering returns all
curl -s -X POST http://localhost:8080/dataio/job-store-service/jobs/searches \
  -H "Content-Type: application/json" \
  -d '{"filtering":[]}' \
  | jq '.[] | {jobId, partitioning: .state.states.PARTITIONING, processing: .state.states.PROCESSING, delivering: .state.states.DELIVERING}'
```

Expected transitions: `PARTITIONING.succeeded` equals the number of chunks once partitioned; `PROCESSING.succeeded` increments as chunks are processed; `DELIVERING.succeeded` increments as chunks are delivered.

### Chunk detail

```bash
curl -s -X POST http://localhost:8080/dataio/job-store-service/jobs/searches \
  -H "Content-Type: application/json" \
  -d "{\"filtering\":[{\"members\":[{\"filter\":{\"field\":\"JOB_ID\",\"operator\":\"EQUAL\",\"value\":\"$NASHORN_JOB_ID\"},\"logicalOperator\":\"AND\"}]}]}" \
  | jq '.[0] | {jobId, chunks: .numberOfChunks, items: .numberOfItems, diagnostics: .state.diagnostics}'
```

### Artemis queue depths

Open http://localhost:8161 in a browser. Log in with `admin` / `GoFish`.
Navigate to **Queues** to see pending messages in `processor::business` and `processor-graaljs::main`.

### Processor health and metrics

```bash
# Health (200 = healthy, 402 = stale queue)
curl http://localhost:8083/health/ready    # job-processor2
curl http://localhost:8084/health/ready    # job-processor-graaljs

# Prometheus metrics
curl http://localhost:8083/metrics
curl http://localhost:8084/metrics
```

### Item log entries

The log-store captures per-item processing logs written by the processors. Fetch the log for a specific item:

```bash
curl -s http://localhost:8085/dataio/log-store-service/logentries/jobs/$NASHORN_JOB_ID/chunks/0/items/0
curl -s http://localhost:8085/dataio/log-store-service/logentries/jobs/$GRAALJS_JOB_ID/chunks/2/items/0
```

Replace `0/0` with the actual `chunkId/itemId`.

---

## 6. Teardown

```bash
# Stop containers, keep volumes (data survives restart)
docker compose -f dev/docker-compose.yml down

# Stop and wipe all data (re-seed on next start)
docker compose -f dev/docker-compose.yml down -v
```

---

## Troubleshooting

**Processors fail to connect to Artemis on startup**
Processors retry JMS connections automatically. Check logs with:
```bash
docker compose -f dev/docker-compose.yml logs job-processor2
docker compose -f dev/docker-compose.yml logs job-processor-graaljs
```

**Job stuck in PROCESSING / chunk not processed**
Check that the processor fetched the JSAR successfully:
```bash
docker compose -f dev/docker-compose.yml logs job-processor2 | grep -i "jsar\|flow\|error"
```
Verify flow ID 1 exists in the flow-store:
```bash
curl -s http://localhost:8081/dataio/flow-store-service/flows/1 | jq '{id, name: .content.name}'
```

**Wrong Nashorn flow ID**
The developer endpoint hardcodes flow ID 1. If the flow-store DB already contains flows from a previous run, the passthrough flow will get a higher ID and the Nashorn processor will fail to find a JSAR. Wipe the stack with `docker compose down -v` and re-seed.

**GraalJS job not routed to GraalJS queue**
Verify the flow binder resolution by checking the Artemis `processor-graaljs::main` queue for pending messages. The GraalJS binder requires `destination: dev-graaljs` to match in the job specification.

**Artemis credentials rejected**
The DBC Artemis image uses `admin` / `GoFish`. If the image version changes, credentials may differ — check the container logs with `docker compose logs artemis`.
