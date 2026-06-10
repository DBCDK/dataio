# job-processor-graaljs

A JMS-based chunk processor that reads `PARTITIONED` chunks from an ActiveMQ Artemis queue,
processes each item using GraalVM JavaScript, and delivers the `PROCESSED` chunk back to the
job-store-service via HTTP.

---

## Environment variables

| Variable             | Required | Default  | Description                                                                          |
|----------------------|----------|----------|--------------------------------------------------------------------------------------|
| `ARTEMIS_MQ_HOST`    | yes      | —        | Artemis broker hostname                                                              |
| `ARTEMIS_JMS_PORT`   | no       | `61616`  | Artemis AMQP port                                                                    |
| `ARTEMIS_USER`       | yes      | —        | Broker username                                                                      |
| `ARTEMIS_PASSWORD`   | yes      | —        | Broker password                                                                      |
| `QUEUE`              | yes      | —        | Incoming queue FQN, e.g. `processor::processor`                                      |
| `JOBSTORE_URL`       | yes      | —        | Base URL of the job-store-service HTTP API                                           |
| `FLOWSTORE_URL`      | no       | `""`     | Base URL of the flow-store-service HTTP API. When blank, flows must carry an embedded JSAR |
| `FLOW_CACHE_SIZE`    | no       | `100`    | Max number of compiled flows held in memory in total; split evenly across `CONSUMER_THREADS` (min 1 per thread) |
| `FLOW_CACHE_EXPIRY`  | no       | `PT10m`  | Flow cache TTL (ISO-8601 duration)                                                   |
| `CONSUMER_THREADS`   | no       | `1`      | Number of parallel JMS consumer threads                                              |
| `WEB_PORT`           | no       | `8080`   | HTTP port for health and metrics endpoints                                           |
| `LOGSTORE_DB_URL`    | yes      | —        | PostgreSQL host:port/dbname for the log-store JDBC appender, e.g. `db-host:5432/logstore` |
| `LOGSTORE_DB_USER`   | yes      | —        | Database username for the log-store JDBC appender                                    |
| `LOGSTORE_DB_PASSWORD` | yes    | —        | Database password for the log-store JDBC appender                                    |

Queue FQN format: `<address>::<queue>`. When address and queue are the same the short form
`<name>` is also accepted (resolved as `<name>::<name>` internally).

---

## Incoming message (job-store → processor)

**Queue:** `$QUEUE`

**JMS message type:** `TextMessage` (AMQP)

### Headers

| Property       | JMS type | Description                                                         |
|----------------|----------|---------------------------------------------------------------------|
| `payload`      | String   | Always `"Chunk"`                                                    |
| `jobId`        | Long     | Job identifier                                                      |
| `chunkId`      | Long     | Zero-based chunk sequence number within the job                     |
| `trackingId`   | String   | Request tracing ID (propagated unchanged to the reply)              |
| `flowId`       | Long     | Flow-store flow ID identifying the JavaScript business logic        |
| `flowVersion`  | Long     | Flow-store flow version                                             |
| `additionalArgs` | String | JSON object with supplementary data for script evaluation, e.g. `{"format":"iso","submitter":870970}` |

### Body

JSON-serialized `dk.dbc.dataio.commons.types.Chunk` with `"type": "PARTITIONED"`.

```json
{
  "jobId": 123,
  "chunkId": 0,
  "type": "PARTITIONED",
  "items": [
    {
      "id": 0,
      "status": "SUCCESS",
      "data": "... base64-encoded record bytes ...",
      "type": "BYTES",
      "trackingId": "abc-123"
    },
    {
      "id": 1,
      "status": "FAILURE",
      "data": null,
      "type": "BYTES",
      "diagnostics": [{ "type": "FATAL", "message": "upstream error" }],
      "trackingId": "abc-124"
    }
  ]
}
```

Only items with `"status": "SUCCESS"` are passed to the JavaScript engine. Items with any
other status are carried through unchanged to the reply with status `IGNORED`.

---

## Reply (processor → job-store)

After processing, the `PROCESSED` chunk is delivered to the job-store via HTTP
(`POST /api/v1/jobs/{jobId}/chunks/{chunkId}`). There is no JMS reply queue.

One output item is produced for every input item, in the same order, with the same `id`.

#### Output item status mapping

| Input status | JavaScript result       | Output status |
|--------------|-------------------------|---------------|
| `SUCCESS`    | Script returns a value  | `SUCCESS`     |
| `SUCCESS`    | Script throws `IgnoreRecord` | `IGNORED` |
| `SUCCESS`    | Script throws `FailRecord` or any other exception | `FAILURE` with diagnostics |
| `FAILURE`    | (not executed)          | `IGNORED`     |
| `IGNORED`    | (not executed)          | `IGNORED`     |

---

## Health and metrics

| Endpoint         | Method | Description                            |
|------------------|--------|----------------------------------------|
| `/health/ready`  | GET    | Returns `200` when healthy, `503` when a fatal condition has been signalled |
| `/metrics`       | GET    | Prometheus-format metrics              |
