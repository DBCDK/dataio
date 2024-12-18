# Special notes about job-store-service

The Service relies on Hazelcast for dependency tracking. The Hazelcast test helpers JetTestSupport and HazelcastTestSupport
Depends on JUnit4 api's. 

**_NOTE:_** The Junit**4** api is used with the full qualifiers `@org.junit.Test` to differenciate it from normal 
JUnit5 api usage.

All Test classes is run either in Junit4 mode or Junit5 mode no Mix and Matching. 