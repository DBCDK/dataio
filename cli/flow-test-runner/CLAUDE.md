# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this tool does

`flow-test-runner` is a CLI tool for locally testing a **Nashorn-based** JSAR (JavaScript ARchive) against pre-recorded expected output files. It runs each scenario's input through the local script and compares the actual output to committed reference files in `expected_state/`. Unlike `acc-test-runner`, it makes no network calls and has no flow-store dependency.

**GraalJS flows are not supported.** The tool uses `ChunkProcessor` from `job-processor2-lib`, which runs Nashorn exclusively. Flows intended for the GraalJS processor will be processed through Nashorn, which may produce different results than production.

## Build and test

```bash
# From project root
mvn package -pl cli/flow-test-runner

# From module directory
mvn package
mvn test
```

Produces a fat JAR (`target/flow-test-runner.jar`) via maven-shade-plugin. The build also strips `logback.xml`, `logback-test.xml`, and `logback-jobstore.xml` from the JAR so logging is user-configurable at runtime.

Tests run with `USE_NATIVE_DIFF=false` (set automatically by surefire config).

## Running the tool

```bash
java -jar target/flow-test-runner.jar <jsar> [dataPath] [-rp <reportPath>] [--packageName <name>]
```

- `<jsar>` — path to the local JSAR file being tested
- `[dataPath]` — root directory to search for `.feature` files (default: `.`)
- `-rp` — JUnit XML report output directory (default: `target/flow-test-runner-reports`)
- `--packageName` — classname prefix for grouping results in CI (e.g. Jenkins)

Returns 0 if all scenarios pass, -1 if any fail.

## Architecture

### Processing pipeline

1. `FlowTestSuite.findFlowTestSuites(dataPath)` — walks the directory tree looking for `test.feature` files. Each file's parent directory becomes one `FlowTestSuite`.
2. For each suite, each `Scenario` is run:
   - Input file is read from `input/` and wrapped in a `Chunk`. `.addi` files are split into one `ChunkItem` per ADDI record; all other files produce a single `ChunkItem`.
   - The chunk is processed by `ChunkProcessor` (from `dataio-job-processor2-lib`, Nashorn-based) using the local JSAR.
   - Actual output is written to `actual_state/<outputFile>`.
   - Expected output is read from `expected_state/<outputFile>` and compared using `MessageConsumerBean` (from `dataio-sink-diff`).
3. Per-suite logback config is programmatically applied: root logger at ERROR, `JavaScript.Logger` at TRACE, both written to `logs/<featureName>.log`.
4. JUnit XML reports are written to the report path.

### Suite directory layout

```
<suite-dir>/           <- named after the feature; becomes the test suite name
  test.feature         <- scenario definitions
  input/               <- input data files referenced in "Når" lines
  expected_state/      <- reference output files committed to VCS
  actual_state/        <- generated during each run (gitignored)
  logs/                <- per-run log files (gitignored)
```

### Feature file format

The `.feature` file format is a Danish Gherkin-inspired DSL (case-insensitive keywords):

```
Egenskab: <description>

  Scenarie: <scenario name>
    # optional comment lines become the scenario description
    Når ... fil <inputFile> ... agency <agencyId> ... format <format> ...
    Så ... fil <outputFile> ...
```

- `Egenskab:` — feature description (the suite name is taken from the directory, not this line)
- `Scenarie:` — starts a new scenario; spaces in the name become underscores in reports
- `Når` line — parsed token-by-token for `fil`, `agency`, and `format` keywords (order-independent)
- `Så` line — parsed for the `fil` keyword to get the expected output filename

### Key classes

| Class | Role |
|---|---|
| `FlowTestRunner` | picocli entry point; orchestrates suite discovery, scenario execution, logging setup, and report writing |
| `FlowTestSuite` | represents one `.feature` file and its directory structure; `FeatureFileParser` parses the Danish DSL |
| `junit.testsuite.*` | JAXB classes generated from `junit-10.xsd` for XML report output |

### Updating test JSARs

To update `src/test/resources/jsar/simple.jsar`:
```bash
cd src/test/resources/jsar/simple.files
# edit files, then:
zip -r ../simple.jsar *
```
