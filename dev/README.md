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

---

## Prerequisites

- Docker + Docker Compose v2 (`docker compose version`)
- Maven 3.9+
- `jq` and `zip`
- Access to `docker-metascrum.artifacts.dbccloud.dk` and `docker-dbc.artifacts.dbccloud.dk`

---

## 1. Build

Build Docker images for the four services (skip integration tests for speed):

```bash
mvn -pl flow-store-service,file-store-service,job-store-service/war,job-processor2/app,job-processor-graaljs \
    -am install -DskipITs
```

Rebuild after code changes with the same command.

---

## 2. Start the stack

```bash
docker compose -f dev/docker-compose.yml up -d
```

Watch startup (Payara services take ~60–90 s):

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

Run once after `docker compose up`. Data is lost on `docker compose down -v`.

The JSARs are pre-built and checked in to `dev/testdata/`. Run the seed script from the **project root**:

```bash
bash dev/scripts/seed-flowstore.sh
```

The script uploads both passthrough flows, creates the submitter and sink, and creates a flow binder for each processor engine. It is idempotent — re-running it on an already-seeded stack detects existing entities and skips them.

> **Note:** The seed script prints the Nashorn and GraalJS flow IDs. Step 5a uses the Nashorn flow ID so the processor can fetch the correct JSAR.

---

## 4. Upload test data to file-store

```bash
FILE_URN=$(curl -s -D - -X POST http://localhost:8082/dataio/file-store-service/files \
  -H "Content-Type: application/octet-stream" \
  --data-binary @dev/testdata/sample-records.ndjson \
  | grep -i "^location:" | sed 's|.*/files/||' | tr -d '[:space:]')

echo "File URN: urn:dataio-fs:$FILE_URN"
```

---

## 5. Submit jobs

### 5a. Nashorn — via developer endpoint (fastest)

The developer endpoint bypasses flow-binder resolution. Pass the actual Nashorn flow ID returned by the seed script so the processor can fetch the JSAR.

```bash
# Look up the Nashorn flow ID (needed so the processor can fetch the JSAR)
NASHORN_FLOW_ID=$(curl -s http://localhost:8081/dataio/flow-store-service/flows \
  | jq '[.[] | select(.name == "Passthrough")] | .[0].id')

JOB=$(curl -s -X POST \
  "http://localhost:8080/dataio/job-store-service/jobs/developer/JSON?flowId=$NASHORN_FLOW_ID" \
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

### 5b. GraalJS — via normal endpoint (full flow-binder resolution)

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

---

## 6. Monitor status

### Job state

The job-store has no `GET /jobs/{id}` endpoint. Queries go through `POST /jobs/searches`:

```bash
# All jobs (compact) — empty criteria returns all
curl -s -X POST http://localhost:8080/dataio/job-store-service/jobs/searches \
  -H "Content-Type: application/json" \
  -d '{"filtering":[]}' \
  | jq '.[] | {jobId, processing: .state.states.PROCESSING, delivering: .state.states.DELIVERING}'

# Single job by ID
curl -s -X POST http://localhost:8080/dataio/job-store-service/jobs/searches \
  -H "Content-Type: application/json" \
  -d "{\"filtering\":[{\"members\":[{\"filter\":{\"field\":\"JOB_ID\",\"operator\":\"EQUAL\",\"value\":\"$NASHORN_JOB_ID\"},\"logicalOperator\":\"AND\"}]}]}" \
  | jq '.[0] | {jobId, processing: .state.states.PROCESSING, delivering: .state.states.DELIVERING}'
```

Expected transitions: `PROCESSING.succeeded` increments as chunks are processed, then `DELIVERING.succeeded` increments as chunks are delivered.

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

---

## 7. Teardown

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
