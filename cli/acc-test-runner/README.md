Acceptance Test Runner
===
Command line tool for running acceptance tests locally. Data partitioning, script processing and diff report generation is all run locally, 
where the product of the supplied script is compared with current flowstore script.

No DataIO jobs will be created as part of this process, nor will any other data be written to production.

### Requirements
Java 11 is needed to run this tool, and is expected to be found in /usr/lib/jvm/java-11 or as the default java version.

### Usage
```
acc-test-runner.sh [-hV] -d=<dependencies> [-f=<flowstore>] [-r=<reportFormat>] -s=<nextScripts> <jobSpec> <recordSplitter> <datafile>
<jobSpec>               job specification file
<recordSplitter>        record splitter <ADDI, ADDI_MARC_XML, CSV|DANMARC2_LINE_FORMAT|DANMARC2_LINE_FORMAT_COLLECTION|
        DSD_CSV|ISO2709|ISO2709_COLLECTION|JSON|VIAF|VIP_CSV|XML|TARRED_XML|ZIPPED_XML>
<datafile>              data file
-d=<dependencies>       Search path for dependencies
-f=<flowstore>          Flowstore url (prod flowstore is default)
    Default: http://dataio-flowstore-service. metascrum-prod.svc.cloud.dbc.dk/dataio/flow-store-service
-h, --help             Show this help message and exit.
-r=<reportFormat>      report format <TEXT|XML>
    Default: TEXT
-s=<nextScripts>       Path to local script
-V, --version          Print version information and exit.
```
### Environment Variables
| Name             |   Options   | Default | Description                                |
|:-----------------|:-----------:|:--------|:-------------------------------------------|
| USE_NATIVE_DIFF  | true, false | true    | Use native scripts to produce the diffs    |
| TOOL_PATH        |             | /work   | Path to jsondiff, plaintextdiff & xmldiff |


### Example 
```
TESTDATA="../../developer-tools/testdata"
JS_SCRIPTS="dam-js"
./acc-test-runner.sh "${TESTDATA}/dmat-staging.jobspec.json" ADDI_MARC_XML "${TESTDATA}/8017302.dmat-staging" \
    -s "${DAM_JS}/publizon-dmat/js/publizon_dmat.js" -d "${DAM_JS}" -r XML
```
