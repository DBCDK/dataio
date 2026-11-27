# 23. Start remote partitioning through the asynchronous EJB method

Date: 2026-11-27

## Status

Accepted

Implemented under DI-3219.

## Context

Only the Hazelcast master partitions jobs. A job added on any other job-store instance is handed to
the master by sending a `RemotePartitioning` task to the master's executor service on the
application's own Hazelcast instance. The master deserialises the task and runs it on a Hazelcast
executor thread. That thread belongs to no Jakarta EE component, so nothing on it knows which
application it is running for.

The task used to look up the default managed scheduled executor and submit the call to
`PgJobStore.partitionNextJobForSinkIfAvailable` there. That method is itself `@Asynchronous`, so
the work ran on the EJB container's async pool either way, and the managed executor only added a
hop in front of it.

Under Payara 7 that hop fails. When a task is submitted, a managed executor records the context of
the submitting thread, and Payara 7 includes OpenTelemetry in that context. OpenTelemetry state is
resolved per application, and on a thread with no component invocation the resolution throws
`IllegalStateException: No application code is executing. Cannot determine current application
scope`. The exception comes from `runAsync` itself, before any future exists. The future returned
by the Hazelcast submission was discarded, so nothing was logged on either instance, and the queue
entry stayed WAITING until a job for the same sink was added on the master, or until a new master
ran `BootstrapBean.resumePartitioning`. In staging every remote request failed this way after the
upgrade.

## Decision

`RemotePartitioning.call()` looks up `PgJobStore` through its `java:global` name and calls
`partitionNextJobForSinkIfAvailable` on that proxy directly. The EJB container accepts the
asynchronous call on the Hazelcast thread, returns at once, and runs the method on its async pool
inside the application's component invocation. A remote request now takes the same path as a job
added on the master itself, where `addJob` calls the same method through `self()`.

A failed lookup or a refused call is logged at ERROR on the master and rethrown. The exception then
reaches the submitting instance through the Hazelcast future, where `Hazelcast.executeOnMaster`
logs it at ERROR as well.

The behaviour is guarded by `RemotePartitioningIT` in `integration-test/job-store-service`. The
test connects to the job-store container's application Hazelcast instance as a client and submits
the task itself. A single container is its own master, so the task arrives on a Hazelcast executor
thread exactly as it does from a second instance. The test holds a job WAITING behind a manually
inserted IN_PROGRESS entry, removes that entry, sends the task, and asserts that the task succeeds
and the job is partitioned.

The real `RemotePartitioning` lives in the war, which the test module cannot depend on. The test
therefore carries a class of the same name and serialised shape, and both declare the same fixed
`serialVersionUID`. The member deserialises the test's bytes as its own class and runs the
production `call()`.

## Alternatives

**Keep the managed executor and propagate less context.** A managed executor defined to propagate
no context might skip the OpenTelemetry provider, but that depends on how Payara orders and applies
its providers rather than on anything in the Jakarta Concurrency contract. `ContextService`
contextual proxies record context from the creating thread in the same way and meet the same
problem.

**Stop doing work on the Hazelcast thread at all.** The task could only record the sink id, for a
poller started from application code on the master to pick up. That removes any dependency on what
the EJB container accepts from a foreign thread, at the cost of a new queue, a poller and a delay
before partitioning starts. It remains the fallback should the direct EJB call stop being accepted.

**A periodic sweep in `resumePartitioning`.** Partitioning every WAITING entry on a schedule would
recover stranded jobs whatever the cause, and would also hide the failure that strands them. It is
not part of this decision. If it is added later, each rescued entry should be logged, so the sweep
reports a fault rather than covering for it.

**Test with a two-instance cluster.** Two job-store containers sharing a DNS name would form a
real cluster and reproduce the production topology. It doubles the heaviest container in the suite
and makes cluster formation a test dependency, for no coverage the single-member arrangement lacks.

**Test through a setting that forces the remote path.** An environment setting that made
`startPartitioner` always use `executeOnMaster` would let a single container exercise the path
through its REST API. It puts a test-only switch into the production image, and nothing would stop
it being enabled in a real deployment.

**Put the war's classes on the test classpath.** Attaching the war's classes as a jar would let the
test use the real `RemotePartitioning` instead of a copy. It is a build change for one test, and it
brings the class's `PgJobStore` dependency and most of the war onto a classpath that needs none of
it.

## Consequences

Remote partitioning works again, and the master's load returns to what it carried before the
Payara 7 upgrade. Partitioning still runs only on the master, on the EJB async pool, through the
unchanged seize query, so the conditions analysed in `docs/postmortem-2026-07-jobstore-jobqueue-stall.md`
and `docs/postmortem-2026-09-jobstore-jobqueue-stall.md` are the same as before the upgrade and so
are their fixes.

The Hazelcast thread now calls into the EJB container. If the container ever ran the call
synchronously, partitioning would run on a Hazelcast executor thread and hold it for the length of
the job. `RemotePartitioningIT` passing does not by itself show which thread did the work. The
container log in that run shows the seize on an `__ejb-thread-pool` thread.

A failed remote request is now visible on both instances, but nothing retries it. The queue entry
stays WAITING until the next partitioning call for its sink, as it did before.

The test-side `RemotePartitioning` must change together with the production class. Every mismatch
fails the test rather than passing it: a different `serialVersionUID` fails deserialisation, a
renamed field leaves `sink` null on the member, and a moved class is not found.

The job-store container in the integration tests exposes the application Hazelcast port 5801, and
the test module depends on the `hazelcast` artifact at test scope.
