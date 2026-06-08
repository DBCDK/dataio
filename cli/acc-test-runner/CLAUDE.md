# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this tool does

`acc-test-runner` is a CLI tool for locally running acceptance tests on **Nashorn-based** DataIO flows before committing them to the flow-store. It compares the output of a **local JSAR** (the candidate script) against the **remote flow** (current production script) using the same input data, and diffs the results. No DataIO jobs are created — everything runs in-process.

**GraalJS flows are not supported.** The tool uses `ChunkProcessor` from `job-processor2-lib`, which runs Nashorn exclusively. Flows targeting the GraalJS processor cannot be tested with this tool.

## Build and test

```bash
# From project root
mvn package -pl cli/acc-test-runner

# From module directory
mvn package
mvn test
```

The build produces a fat JAR (`target/acc-test-runner.jar`) via maven-shade-plugin.

Unit tests run with `USE_NATIVE_DIFF=false` (set automatically by surefire config). Do not set `USE_NATIVE_DIFF=true` when running tests locally unless the native diff tools (`jsondiff`, `plaintextdiff`, `xmldiff`) are available at `TOOL_PATH`.

## Running the tool

```bash
# Download latest release and run (used in CI / by flow developers)
./acc-test-runner.sh [options] <action> <jsar> [dataPath]

# Run from a locally built JAR
java -jar target/acc-test-runner.jar [options] <action> <jsar> [dataPath]
```

The three actions are:
- `TEST` — run tests and write `flow.commit.tmp` (default)
- `COMMIT` — read `flow.commit.tmp` and create/update the flow in flow-store
- `UPLOAD` — skip tests, directly create/update the flow in flow-store

## Architecture

### Processing pipeline (TEST action)

1. `FlowManager.getFlow(jsar)` — reads the local JSAR bytes, wraps them in a `FlowContent`, and tries to resolve the matching flow from the flow-store by name (from the JSAR's `MANIFEST.MF`). If not found by name, constructs a placeholder `Flow(id=1, version=1, content)`.
2. `AccTestRunner.findSuites()` — discovers `.acctest` files under `dataPath` (up to 10 levels deep). Each `.acctest` file is a Java Properties file (`submitterId`, `packaging`, `format`, `destination`, `charset`, `recordSplitter`). The data file is found by name prefix in the same directory.
3. For each suite: `FlowManager.getFlow(jobSpecification)` resolves the remote flow from the flow-store via `FlowBinder` lookup. Both flows must resolve to the same flow ID or the run aborts.
4. Both the local and remote flows are run through `ChunkProcessor` (from `dataio-job-processor2-lib`, Nashorn-based) against the same partitioned data.
5. `MessageConsumerBean` (from `dataio-sink-diff`) diffs the two output chunks item-by-item.
6. `ReportFormat` prints the diff — `TEXT` to stdout, `XML` as JUnit-compatible XML for CI.
7. A `flow.commit.tmp` JSON file is written to `commitPath` (default: `target/`) recording the resolved flow ID/version and whether to CREATE or UPDATE on commit.

### Key classes

| Class | Role |
|---|---|
| `AccTestRunner` | picocli entry point, orchestrates the three actions |
| `AccTestSuite` | parses a `.acctest` spec file and locates its paired data file |
| `FlowManager` | all flow-store interactions (resolve, create, update, commit) |
| `ReportFormat` | TEXT/XML diff output; `junit.testsuite.*` are JAXB classes generated from `junit-10.xsd` |

### Flow resolution logic

The tool tries two resolution paths and uses the result to determine whether the final commit action should be CREATE or UPDATE:

- **By name** (from JSAR MANIFEST.MF) — sets `foundFlowByName = true`
- **By job specification** (packaging + format + charset + submitterId + destination) — resolves via flow-binder lookup
- If neither resolves, the tool assumes this is a **new flow** and proceeds with CREATE
- If both resolve but to different flow IDs, the run is aborted

### Test suite `.acctest` format

```properties
submitterId=123456
packaging=json
format=my-format
destination=my-sink
charset=utf8
recordSplitter=JSON
```

Missing keys fall back to `settings/default.properties` (loaded via `-d` option).

### Updating test JSARs

To update `src/test/resources/jsar/simple.jsar`:
```bash
cd src/test/resources/jsar/simple.files
# edit files, then:
zip -r ../simple.jsar * && cp ../simple.jsar ../simple.old.jsar && zip -j ../simple.old.jsar ../simple.old.files/entrypoint.js
```
