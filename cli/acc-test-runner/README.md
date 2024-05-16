Acceptance Test Runner
===
Command line tool for running acceptance tests locally. Data partitioning, script processing and diff report generation is all run locally, 
where the product of the supplied script is compared with current flowstore script.

No DataIO jobs will be created as part of this process, nor will any other data be written to production.

### Requirements
Java 17 is needed to run this tool, and is expected to be found in /usr/lib/jvm/java-17 or as the default java version.

### Usage
```
Usage: acc-test-runner.sh [-hV] [-cp=<commitPath>] [-f=<flowManager>]
                          [-j=<jobSpec>] [-r=<reportFormat>]
                          [-rp=<reportPath>] [-rs=<recordSplitter>]
                          [-v=<revision>] <action> <jsar> <dataPath>
  <action>                   Action <TEST|COMMIT>, can be either test for running tests or commit for committing
                             previously run tests
                               Default: TEST
  <jsar>                     Path to local script as JavaScript ARchive file (.jsar)
  <dataPath>                 Data path
                               Default: .
  -cp=<commitPath>           Directory for temporary commit file
                               Default: target
  -f=<flowManager>           FlowStore url (prod flowstore is default)
                               Default: http://dataio-flowstore-service.metascrum-prod.svc.cloud.dbc.dk/dataio/flow-store-service
  -h, --help                 Show this help message and exit.
  -j=<jobSpec>               Job specification file
  -r=<reportFormat>          Report format <TEXT|XML>
                               Default: TEXT
  -rp=<reportPath>           Report output path
                               Default: target/reports
  -rs=<recordSplitter>       Record splitter
                               <ADDI|ADDI_MARC_XML|CSV|DANMARC2_LINE_FORMAT|DANMARC2_LINE_FORMAT_COLLECTION|
                               DSD_CSV|ISO2709|ISO2709_COLLECTION|JSON|VIAF|VIP_CSV|XML|TARRED_XML|ZIPPED_XML>
  -v=<revision>              Version
  -V, --version              Print version information and exit.
```
### Environment Variables
| Name             |   Options   | Default | Description                                |
|:-----------------|:-----------:|:--------|:-------------------------------------------|
| USE_NATIVE_DIFF  | true, false | true    | Use native scripts to produce the diffs    |
| TOOL_PATH        |             | /work   | Path to jsondiff, plaintextdiff & xmldiff |
