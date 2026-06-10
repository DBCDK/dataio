# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Component overview

`job-processor2` is a JSE (Java SE) message-driven service that consumes PARTITIONED chunks from the job-store via ActiveMQ Artemis JMS, transforms each record through JavaScript business logic defined in a Flow, and returns PROCESSED chunks to the job-store. It is the second generation processor (replaced the Jakarta EE-based `job-processor`).

## Module layout

- **`lib/`** — all business logic (built as a JAR)
- **`app/`** — thin assembly module: adds `lib` as a dependency, shades into `job-processor2.jar`, builds Docker image

## Build commands

```bash
# Run all unit tests (targets the lib submodule)
mvn test -pl lib

# Run a single test class
mvn test -pl lib -Dtest=ChunkProcessorTest

# Run a single test method
mvn test -pl lib -Dtest=ChunkProcessorTest#callScript
```

## Architecture

### Processing pipeline

```
ActiveMQ Artemis queue
        |
JobStoreMessageConsumer.handleConsumedMessage()   [jms/]
        | extracts Chunk + JMS headers (flowId, flowVersion, additionalArgs)
        v
ChunkProcessor.process()                          [service/]
        | looks up Flow (JSAR) from FlowCache or fetches from job-store
        | sets MDC: flowName, flowVersion
        v
ChunkItemProcessor.processWithRetry()             [util/]   (per item)
        | skips non-SUCCESS items
        | parses ADDI records (content + metadata) or plain data
        | evaluates supplementary JSON via script.eval()
        v
Script.invoke(data, supplement)                   [javascript/]
        | chains through all scripts in the flow
        | returns empty string → IGNORED, throws IgnoreRecord → IGNORED
        |                        throws FailRecord  → FAILURE
        v
Processed ChunkItem (SUCCESS / FAILURE / IGNORED)
        |
JobStoreServiceConnector.addChunkIgnoreProcessed()
```

### Key classes

| Class | Package | Role |
|---|---|---|
| `Jobprocessor` | root | Entry point — extends `MessageConsumerApp`, wires `ServiceHub` |
| `JobStoreMessageConsumer` | `jms` | JMS adapter — handles timeouts, metrics, sends result back |
| `ChunkProcessor` | `service` | Orchestrates per-chunk logic, owns `FlowCache` |
| `ChunkItemProcessor` | `util` | Per-item script invocation, ADDI parsing, exception mapping |
| `FlowCache` | `util` | Guava cache keyed by `[pN:]flowId.flowVersion`; shared or per-instance |
| `Script` (abstract) | `javascript` | Wraps `dk.dbc.jslib.Environment`; subclasses: `JsarScript`, `StringSourceScript` |
| `ProcessorConfig` | root | Env-var config via `EnvConfig` enum |
| `Metric` | root | Prometheus metric definitions |
| `HealthFlag` | `service` | Signals: `OUT_OF_MEMORY` (400), `TIMEOUT` (401), `STALE` (402) |

### Flow / JSAR resolution

Flows are fetched from the job-store connector's cached endpoint. If the flow has no JSAR attached (old format), and `FLOWSTORE_URL` is configured, the JSAR is fetched directly from the flow-store. The resolved `Flow` object is then compiled into `Script` instances and cached.

### Timeout / zombie detection

`JobStoreMessageConsumer` registers a `zombieWatch` check (`scriptRuntimeCheck`) that periodically inspects `scriptStartTimes` — a `ConcurrentHashMap<WatchKey, Instant>`. If any chunk exceeds `HealthService.MAXIMUM_TIME_TO_PROCESS`, the service is marked `TIMEOUT` (health 401) and stops accepting messages.

## Configuration (environment variables)

| Variable | Default | Description |
|---|---|---|
| `QUEUE` | *(required)* | Artemis queue FQN, e.g. `processor::processor` |
| `FLOW_CACHE_SIZE` | `100` | Max entries in the flow cache |
| `FLOW_CACHE_EXPIRY` | `PT10m` | ISO-8601 duration; cache entry TTL |
| `SHARE_FLOWS` | `false` | Share compiled flows across all `ChunkProcessor` instances |
| `FLOWSTORE_URL` | *(optional)* | URL to fetch JSAR from flow-store when not embedded in job-store flow |
| `LOGSTORE_DB_URL` | *(required in prod)* | JDBC URL for logback DB appender |

## Nashorn quirk — retry logic

`ChunkItemProcessor.processWithRetry` retries up to 3 times on `IllegalArgumentException: argument is not an array` — a known Nashorn `RewriteException` instability. On the third failure it throws `ClassCastException` to trigger a processor restart.

## Logging

Production logging uses `logback-jobstore.xml` (passed via `-Dlogback.configurationFile`). It has two appenders:
- **LOGSTORE**: JDBC appender writing to PostgreSQL, gated by `logStoreTrackingId` MDC key (set per chunk item).
- **ASYNC-json**: Async console appender in Logstash JSON format.

Tests use `logback-test.xml` (console only).
