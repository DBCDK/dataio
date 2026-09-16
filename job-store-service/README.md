# Special notes about job-store-service

The Service still runs a Hazelcast node, for the per-sink scheduling counters and the aborted-jobs
set. Chunk scheduling state itself lives in PostgreSQL, see [dependency-tracking.md](dependency-tracking.md).
The Hazelcast test helpers JetTestSupport and HazelcastTestSupport depend on JUnit4 api's. 

**_NOTE:_** The Junit**4** api is used with the full qualifiers `@org.junit.Test` to differenciate it from normal 
JUnit5 api usage.

All Test classes are run either in Junit4 mode or Junit5 mode no Mix and Matching. 