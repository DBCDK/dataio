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

All sinks follow the same structural pattern built on the `jse-artemis` framework:

1. **`*SinkApp`** — entry point, extends `MessageConsumerApp`. Creates a `ServiceHub` and a `Supplier<MessageConsumer>`, then calls `go(serviceHub, messageConsumer)`. Database-backed sinks also call `JPAHelper.migrate()` and `JPAHelper.makeEntityManagerFactory()` here.

2. **`*MessageConsumer`** — extends `MessageConsumerAdapter`. Implements `handleConsumedMessage(ConsumedMessage)`, which is the main processing loop. The standard flow is:
   - `unmarshallPayload(consumedMessage)` → `Chunk` of type PROCESSED
   - Iterate over `ChunkItem`s; handle SUCCESS / FAILURE / IGNORE status
   - Build a new `Chunk` of type DELIVERED
   - `sendResultToJobStore(deliveredChunk)`

3. **`SinkConfig`** — enum implementing `EnvConfig`. Each constant maps to an environment variable. Values are read at startup; default values can be provided in the constructor.

Configuration that varies per job (e.g. endpoint, credentials) comes from the **FlowStore** via `flowStoreServiceConnector`, looked up by `flowBinderId` from the JMS message header. Flow binders are typically cached in a Guava `Cache`.

## Integration tests

Files named `*IT.java` are integration tests run by `maven-failsafe-plugin` during `verify`. They use **Testcontainers** (PostgreSQL) via `PostgresContainerJPAUtils`. Unit tests (`*Test.java`) use Mockito and run with `maven-surefire-plugin` during `test`.

The `testutil` module provides `ObjectFactory.createConsumedMessage(Chunk)` — the standard way to build a `ConsumedMessage` in tests.

## Notable modules

- **`marc-client`** — wsgen-generated JAXWS stubs for the UpdateMarcXchange SOAP service. The `package-info.java` has manually added `@XmlNs` entries to stabilise XML namespace prefixes in tests; preserve these if regenerating.
- **`periodic-jobs`** — most complex sink. Delivers job output via FTP, SFTP, HTTP or email. Uses macro expansion, a week-resolver service, and a JPA-backed database for tracking delivery state.
- **`diff`** — can invoke external tools (`diff`, `jq`, `xmllint`) via `ExternalToolDiffGenerator`; keep that in mind when testing.
- **`util`** — shared library (not a runnable sink). `DocumentTransformer` handles the `dk.dbc.dataio.processing` XML namespace used to embed sink-specific metadata in chunk items.
