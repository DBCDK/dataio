# DataIO

## Project Overview

DataIO is an internal software platform for data ingestion, transformation, and distribution developed and used by our
company DBC.

## Tech stack

- Java 17
- Maven multi-module reactor
- Jakarta EE / Payara Micro 6
- Docker

## Architecture
DataIO is organized around job processing:
- Input data is stored as data files in the file-store component
- These data files, together with other primary job configuration parameters are referenced in job specifications
- Jobs are created by submitting job specifications to the job-store component
- The job-store component partitions jobs into chunks of up to 10 items containing the actual records to be processed
- The job-store uses the flow-store component to determine the actual processing flow and destination to be used for each job
- Each chunk is processed by the job-processor component, which uses JavaScript business logic external to the dataIO system to transform the data
- sinks deliver results from the processing to internal and external systems

## Conventions
- All component paths are relative to the root of the project

# Component paths
| Component                    | Path                                        | Notes       |
|------------------------------|---------------------------------------------|-------------|
| file-store-service           | file-store-service/                         | service     |
| file-store-service-connector | commons/utils/file-store-service-connector/ | client lib  |
| job-store-service            | job-store-service/war/                      | service     |
| job-store-service-connector  | commons/utils/job-store-service-connector/  | client lib  |
| flow-store-service           | flow-store-service/                         | service     |
| flow-store-service-connector | commons/utils/flow-store-service-connector/ | service     |
| job-processor                | job-processor2/                             | service     |
