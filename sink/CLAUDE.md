# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build commands

```bash
# Build all sink modules (from sink/)
mvn package

# Build a single module without Docker
mvn package -pl periodic-jobs -am -Ddocker.build.skip=true

# Run unit tests only (skip integration tests and Docker)
mvn test -Ddocker.build.skip=true

# Run a single test class
mvn test -pl periodic-jobs -Dtest=PeriodicJobsMessageConsumerTest

# Run integration tests (requires Docker for Testcontainers)
mvn verify -pl periodic-jobs

# Regenerate marc-client JAXWS stubs (see marc-client/README.md before doing this)
mvn package -pl marc-client -P generate-source
```

## Architecture

All sinks share the same structure, built on the `jse-artemis` framework:

1. **`*SinkApp`** — entry point, extends `MessageConsumerApp`. Creates a `ServiceHub` and a `Supplier<MessageConsumer>`, then calls `go(serviceHub, messageConsumer)`. Database-backed sinks also call `JPAHelper.migrate()` and `JPAHelper.makeEntityManagerFactory()` here.

2. **`*MessageConsumer`** — the consumer, extending `SinkMessageConsumerAdapter` (epic DI-2946, see `docs/chunk-scheduling-redesign.md`). job-store dispatches one JMS message per item with `payload = ITEM_PAYLOAD_TYPE` and a single `ChunkItem` body, and the sink implements one method:

   ```java
   protected ItemDeliveryResult deliverItem(ConsumedMessage message, ChunkItem item)
   ```

   returning `ItemDeliveryResult.of(status, outcomeItem)`. `handleConsumedMessage` is `final` in the base class. The framework — not the sink — owns the header reads, the delivery watermark check, reporting the result to job-store, the `DBCTrackedLogContext` tracking-id scope, and the `dataio_item_delivery_count` metric. In particular, never read `JMSHeader.recordKey` or re-derive a watermark key from record content.

   Four verdicts, three of them the sink's to return:

   | Verdict | Meaning | DELIVERING counter | Returned by |
   |---|---|---|---|
   | `DELIVERED` | sent to the target | succeeded | the sink |
   | `IGNORED` | not sent, nothing to send | ignored | the sink |
   | `FAILED` | attempted, rejected in a way retrying will not fix | failed | the sink |
   | `SUPERSEDED` | a newer version of the record was already delivered | ignored | the framework only |

   Rules that are easy to get wrong:
   - **Throwing means "retry"** — it rolls the JMS session back and the item is redelivered until the broker gives up. A terminal failure must be returned as `FAILED`, not thrown.
   - **A processing outcome passed through without being sent is `IGNORED`, not `DELIVERED` with an `IGNORE` item.** `DELIVERED` is the only verdict that advances the watermark, so using it for an unsent item both overstates the succeeded count and makes a false claim about what is at the target.
   - **Commit your own writes before returning.** The framework reports the result after `deliverItem` returns, so a reported item is an item whose writes are durable — which is what lets an aggregating sink's job-end work run against complete data.
   - Sinks that aggregate a whole job before delivering anything (`periodic-jobs`, `marcconv`) override `usesDeliveryWatermark()` to `false`. They still report every item individually: the phase counters and the per-job gate are driven by those reports.
   - The job termination item arrives as an ordinary item message carrying `ChunkItem.Type.JOB_END`; sinks needing job-end work branch on that.

   `dlq-errorhandler` and `job-processor2` are not sinks in this sense and stay on the chunk-level `MessageConsumerAdapter` by design: they implement `handleConsumedMessage(ConsumedMessage)` themselves and report whole `Chunk`s via `sendResultToJobStore`.

3. **`SinkConfig`** — enum implementing `EnvConfig`. Each constant maps to an environment variable. Values are read at startup; default values can be provided in the constructor.

Configuration that varies per job (e.g. endpoint, credentials) comes from the **FlowStore** via `flowStoreServiceConnector`, looked up by `flowBinderId` from the JMS message header. Flow binders are typically cached in a Guava `Cache`.

## Integration tests

Files named `*IT.java` are integration tests run by `maven-failsafe-plugin` during `verify`. They use **Testcontainers** (PostgreSQL) via `PostgresContainerJPAUtils`. Unit tests (`*Test.java`) use Mockito and run with `maven-surefire-plugin` during `test`.

There is no shared helper for building a per-item `ConsumedMessage`: build it from a header map (`JMSHeader.payload` = `ITEM_PAYLOAD_TYPE`, plus `jobId`, `chunkId`, `itemId`, `sinkId` and, unless the sink opted out, `recordKey`) and a `JSONBContext`-marshalled `ChunkItem` body. `DummyMessageConsumerTest` and `PeriodicJobsMessageConsumerTest` are the models. Construct the consumer with `new ServiceHub.Builder().withJobStoreServiceConnector(mock).test()` — `test()` rather than `build()`, so no HTTP service is started.

Two things worth knowing before writing such a test:

- **A test that drives `handleConsumedMessage` and then asserts the reported result is re-testing the framework.** Header reading, the watermark comparison and result reporting all live in `SinkMessageConsumerAdapter` and are covered by `SinkMessageConsumerAdapterTest`. A sink's own surface is `deliverItem` plus its `usesDeliveryWatermark()` choice — call `deliverItem` directly. The one exception is asserting the watermark opt-out, which is observable only as the lookup being (or not being) made.
- **A unit test that constructs a consumer needs `APP_NAME`** — `UserAgent.forInternalRequests()` reads it while the consumer builds its connectors, and without it the test fails with "APP_NAME environment variable has not been set". Several modules set it only for failsafe; add the same `<environmentVariables>` block to `maven-surefire-plugin` (see `dpf` and `periodic-jobs`).

## Notable modules

- **`marc-client`** — wsgen-generated JAXWS stubs for the UpdateMarcXchange SOAP service. The `package-info.java` has manually added `@XmlNs` entries to stabilise XML namespace prefixes in tests; preserve these if regenerating.
- **`periodic-jobs`** — most complex sink. Delivers job output via FTP, SFTP, HTTP or email. Uses macro expansion, a week-resolver service, and a JPA-backed database for tracking delivery state.
- **`diff`** — can invoke external tools (`diff`, `jq`, `xmllint`) via `ExternalToolDiffGenerator`; keep that in mind when testing.
- **`util`** — shared library (not a runnable sink). `DocumentTransformer` handles the `dk.dbc.dataio.processing` XML namespace used to embed sink-specific metadata in chunk items.
