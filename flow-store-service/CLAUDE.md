# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Architecture

This is a single-module WAR packaged as a Payara Micro 6 service. It is the configuration registry for DataIO — other services query it to determine which processing flow and sink apply to a given job.

### Core domain model

All entities extend `Versioned` (`entity/Versioned.java`), which stores content as a raw JSON blob in a PostgreSQL `jsonb` column and uses `@Version` for optimistic locking. Clients must pass the current version in the `If-Match` HTTP header when updating or deleting. Concurrent modification returns HTTP 409.

The five primary entities and their tables:

| Entity | Table | Purpose |
|---|---|---|
| `Flow` | `flows` | A JavaScript archive (JSAR blob) that transforms records |
| `FlowBinder` | `flow_binders` | Routing rule: maps (packaging, format, charset, submitter, destination) → Flow + Sink |
| `Submitter` | `submitters` | A library/data supplier identified by number |
| `SinkEntity` | `sinks` | A delivery destination |
| `HarvesterConfig` | `harvester_configs` | Configuration for a harvester instance |

### FlowBinder resolution

`FlowBindersBean.resolveFlowBinder` is the hot path called by job-store-service for every job. It queries `flow_binders` using PostgreSQL's `@>` (JSON containment) operator — the match parameters are serialized to JSON and passed as a native query parameter. A submitter number is first resolved to an internal submitter ID before matching. Errors distinguish between: no submitter found, submitter found but wrong destination, and submitter+destination found but wrong TOC.

### Flow JSAR

Flows store their JavaScript business logic as a binary JSAR (JavaScript Archive) in the `flows.jsar` bytea column (added in `V3__flows_jsar_blob.sql`). `FlowsBean` exposes upload endpoints at `flows/jsar/{last-modified}` (create) and `flows/{id}/jsar/{last-modified}` (update). The optional `FLOWSTORE_FALLBACK` config property points to another flow-store instance to fall back to for JSAR retrieval. `SubversionFetcher` exposes SVN project browsing at `harvesters/svn/...` for fetching JavaScript from Subversion, this is a legacy class, and a candidate for removal.

### REST layer

All EJBs in `ejb/` are also JAX-RS root resources (`@Path("/")`), all registered in `FlowStoreApplication`. URL path constants live in `commons/types/src/main/java/dk/dbc/dataio/commons/types/rest/FlowStoreServiceConstants.java`.

### Persistence

- JPA provider: EclipseLink with Hazelcast cache coordination across cluster nodes
- Migrations: Flyway, scripts in `src/main/resources/db/migration/`
- JNDI datasource: `jdbc/flowStoreDb`, configured via `FLOWSTORE_DB_URL` env var at runtime
- `persistence.xml` registers all entity classes explicitly (EclipseLink requirement)

### Testing

Unit tests use Mockito to mock `EntityManager` directly — there are no integration tests in this module. The `dataio-commons-utils-test` dependency provides JSON builder helpers like `FlowBinderJsonBuilder` and `FlowBinderContentJsonBuilder`.

## Runtime configuration

| Env var | Purpose |
|---|---|
| `FLOWSTORE_DB_URL` | JDBC URL for PostgreSQL |
| `HZ_CLUSTER_NAME` | Hazelcast cluster name for cache coordination |
| `SUBVERSION_URL` | SVN repo base URL for `SubversionFetcher` (default: `NONE`) |
| `FLOWSTORE_FALLBACK` | Optional fallback flow-store URL for JSAR retrieval |
