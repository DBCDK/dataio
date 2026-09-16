# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Run a single integration test class
mvn -pl war verify -Dit.test=PgJobStoreRepositoryIT

# Run only unit tests (failsafe integration tests are skipped)
mvn -pl war test
```

## Module Structure

- `war/` — the deployable service (Payara 6 Micro WAR)
- `types/` — shared types and exceptions used by both this service and its connector (`dataio-job-store-service-types`)
- `test/` — shared test fixtures and builders used by integration tests
- `distributed-objects/` — chunk scheduling state objects, shared between job-store and the scheduler
- `developer-tools/` — Docker Compose stack for local development and a no-op job processor

## Architecture

The job-store-service is the central orchestration point in the DataIO pipeline. It receives job specifications, partitions them into chunks of up to 10 items, schedules processing via message queues, and tracks completion.

### REST layer (`war/src/main/java/.../rs/`)

`JobStoreApplication` registers the JAX-RS beans. The core ones are:
- `JobsBean` — CRUD for jobs, chunks, and items (~40 endpoints)
- `JobsExportsBean` — export endpoints (CSV, line format)
- `NotificationsBean` — job notification management
- `StatusBean` — sink status queries
- `AdminBean` — admin operations (cache flush, retransmit, stale-chunk cleanup)
- `RerunsBean` — triggering job reruns
- `Developer` — enabled only when `DEVELOPER=on`; exposes extra endpoints for local testing

### EJB layer (`war/src/main/java/.../ejb/`)

- `PgJobStoreRepository` — the main DAO; all SQL/JPA access for jobs, chunks, and items goes through here
- `JobSchedulerBean` — schedules chunk processing; dispatches to Artemis queues; has `@Schedule` methods for reaper tasks
- `BootstrapBean` — startup initialization (Hazelcast, Flyway, queue setup)
- `JobProcessorMessageProducerBean` / `SinkMessageProducerBean` — Artemis producers for processor and sink queues
- `JobPurgeBean` / `ScheduledJobPurgeBean` — periodic cleanup of completed jobs
- `JobRerunnerBean` / `RerunsBean` — rerun flow

### Dependency tracking

Prevents chunks from being delivered to a sink out of sequence, and prevents duplicate or orphaned
processing when multiple service instances run. `DependencyTrackingService` (in `war/`) is the entry
point.

See [`dependency-tracking.md`](dependency-tracking.md) for the mechanism. It is part of the
scheduling redesign in `docs/chunk-scheduling-redesign.md`, whose Phase 9 records how much has
merged.

### Database

PostgreSQL + Flyway migrations under `war/src/main/resources/db/migration/`. JPA persistence unit `jobstorePU` uses EclipseLink with Hazelcast L2 cache coordination.

## Testing Notes

Integration tests use Testcontainers (PostgreSQL). The base class is `AbstractJobStoreIT`.

**JUnit4 / JUnit5 coexistence** — Hazelcast test helpers (`JetTestSupport`, `HazelcastTestSupport`) depend on JUnit 4. To avoid ambiguity, JUnit 4 annotations are always written with full qualifiers: `@org.junit.Test`. All test classes run in either JUnit 4 mode **or** JUnit 5 mode; never mixed in the same class.

When implementing significant changes in the job-store-service, remember to also run container tests in
the integration-test/job-store-service module. Also investigate if more tests are needed in those test suites.

## Local Development

The `developer-tools/` module provides a Docker Compose stack:

```bash
cd developer-tools/src/main/resources/docker-compose
docker compose up -d
```

This starts the service on port 8080 (debug on 9009), PostgreSQL on 5432, and ActiveMQ Artemis. Set `DEVELOPER=on` to enable the developer-only REST endpoint used for injecting test jobs. See `developer-tools/README.md` for curl examples.

## Key Environment Variables

| Variable | Purpose                                                                     |
|---|-----------------------------------------------------------------------------|
| `JOBSTORE_DB_URL` | JDBC URL for PostgreSQL                                                     |
| `ARTEMIS_MQ_HOST` | ActiveMQ Artemis host                                                       |
| `FLOWSTORE_URL` | Flow store service URL                                                      |
| `FILESTORE_URL` | File store service URL                                                      |
| `PROCESSOR_TIMEOUT` | ISO-8601 duration before a chunk is considered stale (default `PT1H`)       |
| `JOBQUEUE_STUCK_THRESHOLD` | ISO-8601 duration a job queue entry may stay `IN_PROGRESS` before `JobQueueWatchdogBean` reports it (required, set to `PT1H` in the Dockerfile) |
| `PROCESSOR_NASHORN_QUEUE` | Artemis queue for the Nashorn processor (default `processor::business`)     |
| `PROCESSOR_GRAALJS_QUEUE` | Artemis queue for the GraalJS processor (default `processor-graaljs::main`) |
| `DEVELOPER` | Set to `on` to enable developer endpoints                                   |
| `WATERMARK_RETENTION` | ISO-8601 duration a `sink_record_delivery_watermark` row may go unmodified before nightly pruning removes it (default `P90D`) |
