# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build and run tests (from this module directory):
```
mvn verify
```

Run a single test class:
```
mvn test -Dtest=HarvestOperationTest
```

Run a single test method:
```
mvn test -Dtest=HarvestOperationTest#harvestOneRecord
```

Build without Docker image:
```
mvn verify -Ddocker.build.skip=true
```

Build from the repo root to include all dependencies:
```
mvn verify -pl harvester/dmat-dm3 -am
```

## Architecture

This harvester bridges the DMat system (digital materials catalog for ebooks/eaudiobooks) to the DataIO job pipeline. It polls DMat for records in `PENDING_EXPORT` status and submits them as DataIO jobs.

### Harvest flow

`ScheduledHarvestBean` fires every minute and delegates to `HarvesterConfigurationBean` (fetches `DMatDM3HarvesterConfig` from flow-store) and `HarvesterBean` (the EJB singleton that owns HTTP client setup). `HarvesterBean.executeFor()` constructs a `HarvestOperation` and calls `.execute()`.

`HarvestOperation.execute()` does the following in order:
1. Pages through DMat records using the inner `ResultSet` iterator (fetch size 100, cursor-based via `from` offset)
2. Immediately sets each record to `PROCESSING` in DMat to prevent double-export
3. Validates each record's `updateCode`/`selection` combination via `assertRecordState()`
4. Fetches the attached rawrepo record via `RecordFetcher` — the record fetched depends on the combination of `updateCode` and `selection` (see table below)
5. Assembles an Addi record: `ExtendedAddiMetaData` as JSON metadata (serialised with `RecordView.Export` Jackson view) + MarcXchange collection as content
6. Builds a DataIO job via `JobBuilder` (submitter 190015, packaging `addi-xml`)
7. After job creation succeeds, updates statuses in DMat to `EXPORTED` (or back to `PENDING_EXPORT` on failure)
8. Updates `timeOfLastHarvest` in flow-store config

### Record routing logic (`RecordFetcher`)

| UpdateCode    | Selection       | Record fetched from rawrepo (agency 191919)   |
|---------------|-----------------|-----------------------------------------------|
| NEW, AUTO     | CREATE          | None (empty MarcXchange collection)           |
| NEW, AUTO     | CLONE           | `DMatRecord.match` (the matched faust number) |
| ACT           | CREATE          | None                                          |
| NNB           | DROP, AUTODROP  | None                                          |
| REVIEW        | any             | `DMatRecord.reviewId`                         |
| UPDATE        | any             | `DMatRecord.recordId`                         |

All rawrepo fetches use EXPANDED mode with `keepAutFields=true`, `useParentAgency=true`, `expand=true`.

### Environment variables

| Variable                  | Purpose                                  |
|---------------------------|------------------------------------------|
| `DMAT_SERVICE_URL`        | Base URL for DMat REST API               |
| `DMAT_DOWNLOAD_URL`       | printf-format URL for DMat content links (`%s` = faust number) |
| `RAWREPO_RECORD_SERVICE_URL` | Base URL for rawrepo record service   |
| `TZ`                      | Timezone for creation date calculation (defaults to `Europe/Copenhagen`) |

### Status lifecycle in DMat

`PENDING_EXPORT` → `PROCESSING` (set immediately on fetch) → `EXPORTED` (on success) or `PENDING_EXPORT` (on failure)

If the harvester crashes after setting `PROCESSING` but before completing, records are left stranded in `PROCESSING` — this is logged explicitly as a warning.

### Key design notes

- The `creationDate` on the Addi metadata is set to noon Copenhagen time of the DMat export date. Flowscripts consuming these jobs **must** use `formattedCreationDate` from the DMat export object rather than the Addi timestamp, due to timezone offset behaviour inherited from the DataIO standard.
- `HarvestOperation.DMAT_SERVICE_FETCH_SIZE` is package-private to allow tests to override it (set to 2 in `HarvestOperationTest` to force multi-page iteration).
- HTTP clients for DMat and rawrepo are configured with a retry policy (3 retries, 5s delay) on 404/500/502 and connection failures.
