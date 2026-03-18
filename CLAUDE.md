# DataIO

## Project Overview

DataIO is an internal software platform for data ingestion, transformation, and distribution developed and used by our
company DBC.

## Technical details

The programming language used is Java version 17.

The project is structured as a large Maven multi-module reactor. Some modules are micro-services based on the
Payara micro server version 6 intended to be deployed as Docker containers in a Kubernetes cluster, some modules are
utility libraries, and some modules are command line applications.

## System Design

DataIO works with a job abstraction. The actual raw input data is stored as data files handled by the file-store-service
component. These data files, together with other primary job configuration parameters are then referenced in job
specifications, which are then added to the job-store-service component. The job-store-service component partitions
jobs into chunks of up to 10 items containing the actual records to be processed. The job-store-service component
uses the flow-store-service component to determine the actual processing flow and destination to be used for each job.
Each chunk is processed by the job-processor component, which uses JavaScript business logic external to the dataIO 
system to transform the data. Various sink components are responsible for the delivery of the transformed data to
services both internal and external to DBC.