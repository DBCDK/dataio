# Postmortem: Job Queue Stall - Unwakeable EclipseLink Cache-Key Wait

**Service:** job-store-service
**Dates:** 2026-09-04 through 2026-09-16, recurring
**Severity:** P2 (no thread-pool exhaustion, isolated to one submitter at a time)
**Status:** Root cause established. Fixes proposed and still not shipped. Recurs on every restart.

> **Revision note (2026-09-15).** The first version of this document described the failure as a
> two-thread deadlock, following the model in the July postmortem. A second capture taken after
> manual recovery disproved that. The failure is a **leaked cache-key lock**, not a deadlock. The
> root cause analysis below has been rewritten. The correction is recorded rather than quietly
> edited out, because the wrong model would have led to the wrong fix ranking.

> **Revision note (2026-09-16).** The second version described the failure as a leaked cache-key
> lock whose holder could not be identified, and listed identifying it as an open action. There is
> nothing to identify. Disassembly of `org.eclipse.persistence.core.jar` taken out of the running
> container shows that `WriteLockManager.acquireLocksForClone` waits on the CacheKey's **intrinsic
> object monitor**, that `ConcurrencyManager.release` signals a **`ReentrantLock` `Condition`**,
> and that not one of the 2,113 classes in that jar contains the string `notify` at all. The wait
> is therefore unwakeable by construction. The failure is a **lost notification**, not a leak and
> not a deadlock. Root cause, fix ranking and action items are rewritten again below. Both
> superseded models are kept on the record for the same reason as before, and because each one
> produced a different and wrong answer to "what do we ship".

---

## Summary

EJB pool threads park permanently inside EclipseLink's `WriteLockManager.acquireLocksForClone`.
In the Payara-patched EclipseLink build we run, that method waits on the CacheKey's intrinsic
object monitor while `ConcurrencyManager.release` signals a `ReentrantLock` `Condition`. The two
are separate wait queues, and no class in the jar ever calls `notify` or `notifyAll` on anything.
A thread that reaches the wait cannot be woken by a release. Because the wait is also untimed
(`eclipselink.concurrency.manager.waittime` defaults to `0`) it never ends at all.

Any time two threads genuinely contend on the same cache key inside a cascaded clone, the loser
parks forever. Each parked thread leaves one job queue entry in `IN_PROGRESS`, and PR #295's
prior-entry guard then blocks every later entry for that sink and submitter.

At least five parked threads between 2026-09-04 and 2026-09-16: one before 09-04 inferred from the
backlog, then 09-09, 09-11, 09-15 and 09-16. Four of the five are on sink 13451 and one on sink
4213. Every manual recovery has re-triggered it within minutes, because a restart drains the
accumulated backlog through the contended path at maximum rate. A restart is the trigger, not the
cure.

It went unnoticed for days because nothing reads `JobQueueRepository.getInProgress()` at runtime,
and the affected sinks kept processing other submitters normally.

---

## Timeline

### Occurrences before 2026-09-09

The event below is not the first. At the 2026-09-09 11:13:03 pod start the backlog drained back
to job 38283416, created 2026-09-04 08:44, which completed 1.4 seconds after `BootstrapBean`
jump-started sink 13451:

```
[2026-09-09T11:14:02.779] PgJobStore TIMER jobId=38283416 numberOfItems=27 numberOfChunks=3
    submitterNumber=820120 destination=solr-docstore timeOfCreation=1788504261222
```

Roughly 3,200 distinct sink 13451 jobs for submitter 800010 and 447 for submitter 820120
completed between 11:13 and 11:29, and the drain then reached entry 36367474 and hung there. So
sink 13451 was already stalled on 2026-09-04 and the event below is at least the third
occurrence. This is also what explains the otherwise unexplained three day gap in the table
below, where entry 36367474 is inserted on 09-06 and not seized until 09-09.

### First captured occurrence

| Time | Event |
|---|---|
| 2026-09-06 08:24:41 | Queue entry 36367474 (sink 13451, job 38304622) inserted, state `WAITING`. It is not seized, because the sink is already stalled behind the earlier occurrence. |
| 2026-09-09 11:13:03 | Pod starts. Payara Micro 6.2025.11 (build 43). |
| 2026-09-09 11:14:02 | `BootstrapBean resumePartitioning(): found 1 sinks to jump-start`, sink 13451 (Cisterne solr-doc-store holdings). The backlog begins to drain. |
| 2026-09-09 11:23:01 | The drain reaches entry 36367474. `__ejb-thread-pool11` seizes job 38304622 and begins partitioning. |
| 2026-09-09 11:23:08.865 | Partitioning **succeeds**. `PgJobStore TIMER jobId=38304622 numberOfItems=1000 numberOfChunks=100` is logged. This is the thread's last log line, ever. |
| 2026-09-09 11:23:08+ | The success path calls `jobQueueRepository.remove(jobQueueEntity)` (`PgJobStore.java:269`). The thread blocks inside `entityManager.merge` and never returns. Entry 36367474 stays `IN_PROGRESS`. |
| 2026-09-09 11:24:18 | Job 38304622 finishes delivery normally. The job is complete, only its queue entry is orphaned. **Sink 13451 stalled for this submitter.** |
| 2026-09-11 12:58:13.717 | Burst on sink 4213. Entry 36400113 (job 38337322) inserted. It is never seized. Job 38337322 appears exactly once in the entire 438 MB log, on its `addJob` line. |
| 2026-09-11 ~13:00:45 | `__ejb-thread-pool6` runs the seize query, acquires the `FOR UPDATE OF jq` row lock, and blocks inside `getResultList`. Its transaction never commits. **Sink 4213 stalled for this submitter.** |
| 2026-09-11 to 2026-09-15 | Entries accumulate behind both head entries. Both sinks continue to process other submitters, so no symptom is visible externally. No alert exists to fire. |
| 2026-09-15 07:18 - 07:32 | First diagnostic capture. 327 queue entries, 1 `IN_PROGRESS` and 326 `WAITING`. |

### Manual recovery and recurrence

| Time | Event |
|---|---|
| 2026-09-15 ~08:45 | Manual recovery: orphaned entry 36367474 deleted, pod restarted. Deleting before the restart avoids `ensureLastChunkIsScheduled` re-delivering chunk 99 of the completed job, see "Recovery hazard" below. |
| 2026-09-15 08:46:40 | Pod starts. Hazelcast DNS discovery warnings for `dataio-jobstore-cluster`, unrelated to this failure. |
| 2026-09-15 08:46:52 - 08:49:26 | Backlog drains. Sink 4213 fully clears all 49 entries. Sink 13451 clears 275 of 278. |
| 2026-09-15 08:47:06 - 08:48:01 | Three transient `IllegalArgumentException: item can not be null` failures reading back PROCESSED chunks (jobs 38304642/63, 38304755/88, 38304798/98). All three retry and complete by 08:54. Unrelated to the stall, noted below. |
| 2026-09-15 08:49:26.751 | `__ejb-thread-pool7` seizes job 38341335 (sink 13451, submitter 800010, 1 item, 1 chunk). |
| 2026-09-15 08:49:26.842 | Partitioning **succeeds** in 91 ms. `remove()` is called and the thread blocks. Last line from that thread. **Sink 13451 stalled again, three minutes after restart.** |
| 2026-09-15 08:49:31.700 | Job 38341335 delivers and completes. Queue entry 36404110 orphaned in `IN_PROGRESS`. |
| 2026-09-15 08:54:16 | Second diagnostic capture. 3 queue entries remain, all sink 13451. |

Note that the restart reproduced the failure by design rather than by bad luck.
`BootstrapBean.resumePartitioning()` logged `found 1 sinks to jump-start` and named sink 13451 in
both captures, at 11:14:02 and at 08:47:00 respectively. It is the only sink that gets jump-started,
and the hang lands inside the drain it starts.

### Second recurrence

| Time | Event |
|---|---|
| 2026-09-16 morning | Stalled again, same sink, reported by operations. No capture taken. None of the fixes below had been deployed. |

---

## Impact

| Metric | First captured occurrence | Recurrence |
|---|---|---|
| Undetected stall, sink 13451 | ~5 days 20 hours, on top of an earlier stall running from 2026-09-04 | ongoing at capture |
| Undetected stall, sink 4213 | ~3 days 18 hours | not affected |
| Queue entries backed up at capture | 326 `WAITING` plus 1 `IN_PROGRESS` | 2 `WAITING` plus 1 `IN_PROGRESS` |
| Jobs delayed, drained on restart | ~3,600 on sink 13451 | 324 |
| EJB pool threads lost | 2 of 16 | 1 of 16 |
| Thread-pool exhaustion | No | No |
| Other sinks affected | None | None |

Unlike July, no occurrence exhausted the thread pool. Partitioning continued normally for
every other sink and submitter throughout. That is precisely why these have run for days
without being noticed.

Recovery requires deleting the orphaned queue row and restarting the pod, and it is not durable.
Time to recur was 10 minutes on 2026-09-09 against a ~3,600 job backlog and under 3 minutes on
2026-09-15 against 324 entries. The larger the backlog allowed to build up, the denser the drain
and the sooner it hangs again.

---

## Root Cause Analysis

### What is stuck

Both captures show the same thing. The second is the clearer of the two because it contains only
one affected thread.

| Capture | Thread | Blocked in | Since |
|---|---|---|---|
| 1 | `__ejb-thread-pool11` | `JobQueueRepository.remove` -> `entityManager.merge` (`JobQueueRepository.java:49`, from `PgJobStore.java:269`) | 2026-09-09 11:23:08 |
| 1 | `__ejb-thread-pool6` | `seizeHeadOfQueueIfWaiting` -> `getResultList` (`JobQueueRepository.java:84`, from `PgJobStore.java:233`) | 2026-09-11 ~13:00:45 |
| 2 | `__ejb-thread-pool7` | `JobQueueRepository.remove` -> `entityManager.merge`, same frames as pool11 | 2026-09-15 08:49:26 |

All bottom out in the same frames, shown here from capture 2:

```
java.lang.Thread.State: WAITING (on object monitor)
    at java.lang.Object.wait0(java.base@21.0.2/Native Method)
    - waiting on <no object reference available>
    at java.lang.Object.wait(java.base@21.0.2/Object.java:366)
    at org.eclipse.persistence.internal.helper.WriteLockManager.acquireLocksForClone(WriteLockManager.java:184)
    - locked <0x00000006099c2b48> (a HardCacheWeakIdentityMap$ReferenceCacheKey)
```

The monitor address differs per thread and per capture, which matters. See below.

In capture 1, `__ejb-thread-pool6` is PostgreSQL backend `1330884`: `idle in transaction`,
`wait_event = ClientRead`, `3 days 18:26:15`, running the seize query. PostgreSQL had finished
the `SELECT`, returned the rows, and was waiting for a client that was never coming back. The
`FOR UPDATE OF jq` row lock on entry 36400113 was held that entire time.

Note what is **not** present in either capture: no `BLOCKED` threads, no ungranted locks, and the
blocked/blocking pair query returns 0 rows. Neither thread was ever waiting on PostgreSQL.

### It is a lost wakeup, not a leak and not a deadlock

Capture 2 is decisive on the negative half. **`__ejb-thread-pool7` is the only thread in the
entire JVM with any `org.eclipse.persistence` frame on its stack.** There is no counterparty.
Capture 1 adds that its two blocked threads own two **different** CacheKeys:

```
__ejb-thread-pool6   - locked <0x000000060a6b3d80> (a HardCacheWeakIdentityMap$ReferenceCacheKey)
__ejb-thread-pool11  - locked <0x000000060a6b4da0> (a HardCacheWeakIdentityMap$ReferenceCacheKey)
```

Two independent victims, not a pair. No cycle exists in either capture.

The previous version of this document inferred from that a leaked `activeThread`. Disassembling
the jar shows the inference was unnecessary, and wrong. `activeThread` is almost certainly `null`
in both dumps. The counterparty released normally, microseconds after the victim began waiting,
and the victim never heard about it.

**The wait and the wake-up are on two different locks.**

`WriteLockManager.acquireLocksForClone`, bytecode from the production jar:

```
161: monitorenter                                      <- synchronized (toWaitOn)
164: invokevirtual CacheKey.isAcquired:()Z
167: ifeq          181
175: invokevirtual ConcurrencyUtil.getAcquireWaitTime:()J
178: invokevirtual java/lang/Object.wait:(J)V          <- parks on the intrinsic monitor
231: monitorexit
```

`ConcurrencyManager.release`, same jar:

```
  4: invokeinterface java/util/concurrent/locks/Lock.lock:()V       <- instanceLock
 27: invokevirtual  AtomicInteger.decrementAndGet:()I               <- depth
 43: putfield       activeThread:Ljava/lang/Thread;                 <- cleared to null
 66: invokeinterface java/util/concurrent/locks/Condition.signalAll <- instanceLockCondition
 75: invokeinterface java/util/concurrent/locks/Lock.unlock:()V
```

`ConcurrencyManager` carries `private final Lock instanceLock` and `private final Condition
instanceLockCondition` and uses them for all of its state. `WriteLockManager` still uses the
legacy `synchronized` plus `wait` form on the same objects. A `Condition.signalAll` does not wake
a thread sitting in `Object.wait`.

The decisive check, run against the jar copied out of the running container:

```bash
# 2113 classes in org.eclipse.persistence.core
grep -rl notify --include='*.class' .     # no output at all
```

Not one class contains the string `notify`. No `notifyAll`, no `notify`, and no `monitorenter`
anywhere in `ConcurrencyManager`. Nothing in EclipseLink ever signals a CacheKey's intrinsic
monitor. A thread that reaches offset 178 cannot be woken by a release, by any thread, ever.

The `isAcquired()` check at offset 164 is meant to be the guard against precisely this. It is not
one, because `release` never takes the CacheKey's monitor, so `synchronized (toWaitOn)` does not
serialise against `release` at all. Surviving comes down to whether the holder happens to release
in the few microseconds between `acquireNoWait` failing inside `acquireLockAndRelatedLocks` and
the `isAcquired()` re-check. Usually it does not.

End to end:

1. Two threads contend on the same cache key inside a cascaded clone.
2. `acquireLockAndRelatedLocks` fails `acquireNoWait` and returns the contended CacheKey as `toWaitOn`.
3. The loser enters `synchronized (toWaitOn)`, sees `isAcquired()` still true, and calls `wait(0)`.
4. The winner finishes and calls `release`, which clears `activeThread` and signals the Condition.
5. Nobody signals the monitor. The loser is parked for the life of the JVM.

This supersedes two earlier models:

- The July postmortem's cross-system deadlock, PostgreSQL row lock versus cache-key lock.
- The first version of this document's two-thread deadlock between seize and remove.
- The second version of this document's leaked `activeThread`.

The first two were consistent with capture 1, which happened to contain two blocked threads. The
third was consistent with both captures, and was reasoning about a field a thread dump cannot
show. Reading the bytecode of the loaded artefact settled it, and nothing short of that would
have.

### The counterparty in capture 2

The stack names the entity path:

```
JobQueueRepository.remove(JobQueueRepository.java:49)
  EntityManagerImpl.merge
    UnitOfWorkImpl.mergeCloneWithReferences
      MergeManager.registerObjectForMergeCloneIntoWorkingCopy
        UnitOfWorkIdentityMapAccessor.getAndCloneCacheKeyFromParent
          UnitOfWorkImpl.cloneAndRegisterObject
            WriteLockManager.acquireLocksForClone     <- parked here
```

`mergeCloneWithReferences` on a detached `JobQueueEntity` cascades through the non-indirect `job`
relation onto `JobEntity` 38341335. The log shows another thread inside that same entity in the
same 40 ms window:

```
08:49:26.808 [http-thread-pool::http-listener(36)] PgJobStore addChunk: adding PROCESSED chunk 38341335/0
08:49:26.842 [__ejb-thread-pool7]                  PgJobStore TIMER jobId=38341335 ...
                                                   then remove(), then parked
```

Every chunk path goes through `getExclusiveAccessFor(JobEntity.class, jobId)`
(`PgJobStoreRepository.java:341`, `:423`, `:463`, `:818`), a `PESSIMISTIC_WRITE` find that takes
the write lock on that cache key. That is the contention. For a one item, one chunk job, where
partitioning finishes in the same instant the chunk is added, it is structural rather than
unlucky.

### Why the wait never ends

The lost notification is what makes the thread miss its wake-up. The untimed wait is what makes
that permanent rather than a 40 second stall.

`ConcurrencyUtil` defaults, confirmed in the jar (`acquireWaitTime` is initialised from
`getLongProperty(name, 0L)`):

```java
private static final long DEFAULT_ACQUIRE_WAIT_TIME = 0L;
private static final long DEFAULT_MAX_ALLOWED_SLEEP_TIME_MS = 40000L;
private static final boolean DEFAULT_INTERRUPTED_EXCEPTION_FIRED = true;
```

`getAcquireWaitTime()` returns `0`, so offset 178 is `Object.wait(0)`. That matches the
`Object.java:366` frame in both captures.

EclipseLink ships a breaker for threads frozen in this method, and its own source comment shows
the authors knew about the hang:

```java
// Since we know this one of those methods that can appear in the dead locks
// we threads frozen here forever inside of the wait that used to have no timeout
// we will now always check for how long the current thread is stuck in this while loop
ConcurrencyUtil.SINGLETON.determineIfReleaseDeferredLockAppearsToBeDeadLocked(
        toWaitOn, whileStartTimeMillis, lockManager, readLockManager, true);
```

It runs **before** the wait, once per iteration of the `while (toWaitOn != null)` loop. With an
untimed wait the thread never completes an iteration, so the detector fires once at time zero,
when `whileStartTimeMillis` is fresh and nothing looks wrong, and then the thread parks forever.
The escape hatch cannot be reached.

Set the wait to a non-zero value and the two defects cancel. The thread wakes on the timeout
rather than on a signal it will never get, re-enters the loop, retries
`acquireLockAndRelatedLocks`, finds the lock long since released, and proceeds. The missing
notification stops mattering.

The same pattern appears in `ConcurrencyManager.acquireReadLock`, where
`instanceLockCondition.await(getAcquireWaitTime(), MILLISECONDS)` with a zero timeout is likewise
an unbounded wait. That one at least awaits the condition the release actually signals, so it is
only a missing bound and not a missing wake-up, and the detector there is inside the loop.

### What this most likely is

`ConcurrencyManager` has been converted to `ReentrantLock` and `Condition` throughout.
`WriteLockManager.acquireLocksForClone` has not. Upstream EclipseLink 4.0.7 converted both, which
is why upstream does not exhibit this failure. The running artefact is
`4.0.7.payara-p1v202509010857`, a Payara-patched build that carries the new `ConcurrencyManager`
and the old `WriteLockManager`. That reads as a partial backport whose two halves do not
interoperate.

This last step is inference from the binary rather than from Payara's source. What is not
inference is that the jar we run contains a release path signalling a condition that no wait path
awaits, and a wait path on a monitor nothing signals.

### Why the July fixes did not prevent this

The July postmortem models the cycle as PostgreSQL row lock versus EclipseLink cache-key lock,
and both fixes act on the PostgreSQL side:

- PR #294 narrowed `FOR UPDATE` to `FOR UPDATE OF jq`.
- PR #295 added `jq.state = 'WAITING'`, the prior-entry guard, and `SKIP LOCKED`, making seize
  and remove row-disjoint.

Both are correct and both still hold. Neither is relevant here, because **no PostgreSQL lock is
part of this failure**. The row lock held by the seize in capture 1 is a consequence of the hang,
not a cause of it. An unwakeable in-process wait is invisible to any amount of SQL predicate
work.

Two entity properties put `JobQueueEntity` on the affected path:

1. `persistence.xml` sets `<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>` and no
   entity in the persistence unit carries `@Cacheable(false)`, so everything is shared-cached.
2. `JobQueueEntity.job` is `@ManyToOne(fetch = FetchType.EAGER)`. In
   `ClassDescriptor.postInitialize`:

   ```java
   if (mapping.isForeignReferenceMapping()){
       if (!((ForeignReferenceMapping)mapping).usesIndirection()){
           setShouldAcquireCascadedLocks(true);
       }
   ```

   A non-indirect foreign reference sets `shouldAcquireCascadedLocks = true`, which is what sends
   the clone into `acquireLocksForClone` at all rather than the cheap single read-lock path, and
   what makes the traversal (`traverseRelatedLocks` -> `checkAndLockObject`) also try to lock the
   referenced `JobEntity` cache key.

Of the four jobstore entities with relationships, only `JobQueueEntity` and `RerunEntity` have a
non-LAZY one, so those two are the only descriptors that can enter `acquireLocksForClone` as the
root object. `JobEntity`'s own two relationships are both `LAZY`.

That cascade is also why the contention exists at all. The clone of a `JobQueueEntity` reaches
for the `JobEntity` cache key, which is the one entity every chunk and delivery path in the
service locks exclusively. Without the cascade there would be nothing to contend on, and
`acquireLocksForClone` would not be entered in the first place.

Note that the cache key is contended, not permanently held. In capture 2 the job's own
`JobEntity` was demonstrably usable after the hang began, since
`addChunk: adding DELIVERED chunk 38341335/0` and the completion `TIMER` both succeeded at
08:49:31, five seconds after `__ejb-thread-pool7` blocked. Under the leak model that observation
was awkward. Under the lost-wakeup model it is exactly what is expected: the lock was released on
schedule and the entity went on being used normally, while one thread slept through the
notification.

### The amplifier: one wedged entry stalls a whole submitter

PR #295's prior-entry guard is what turns a single parked thread into a multi-day queue stall:

```sql
AND NOT EXISTS (
  SELECT 1 FROM jobqueue prior INNER JOIN job pjob ON prior.jobid = pjob.id
  WHERE prior.sinkid = jq.sinkid AND prior.id < jq.id
    AND pjob.specification->>'submitterId' = job.specification->>'submitterId')
```

An older entry for the same sink and submitter excludes every later one, **in any state**. So:

- **Sink 13451, every occurrence**: the head entry is `IN_PROGRESS` forever, so every later entry
  is excluded.
- **Sink 4213, first occurrence**: entry 36400113 was still `WAITING` but row-locked by the
  zombie transaction. `SKIP LOCKED` steps over it, and then the guard excludes everything behind
  it because the older row still **exists**. Presence, not lock state, is the criterion.

The query comment states that `SKIP LOCKED` is "safe only because (2) is in place. Concurrent
seizes skip locked head rows and take a different submitter's head instead of queuing behind the
lock." That reasoning holds for transient locks. It does not hold for a lock that is never
released, where the guard converts a skipped row into a permanent head-of-line block.

This is not an argument that the guard is wrong. It upholds a real ordering requirement. It is an
argument that its failure mode under a stuck entry is unbounded and silent, and that it deserves
a bound.

### Why a restart re-triggers it, and why it is always sink 13451

The hang needs two threads on one cache key, which ordinary traffic rarely produces. Sink 13451
produces it reliably:

- Submitter 800010 sends one item, one chunk jobs that partition in 91 ms, so `remove()` on the
  queue entry and `addChunk` on the job entity fire in the same instant on the same `JobEntity`
  key, on every single job.
- The re-trigger at `PgJobStore.java:281` runs seize, partition, remove back to back with nothing
  in between, so those instants come as fast as the sink can produce them.
- Everything runs on the Hazelcast master since March 2024, so both sides always share one
  identity map.
- The traffic arrives in batches. 210 of the 220 queue entries dated 2026-09-13 were created
  inside the 04:00 hour.

A restart then concentrates all of it. `BootstrapBean.resumePartitioning()` jump-starts exactly
one sink, 13451 in both captures, and pushes the entire accumulated backlog through that path at
maximum rate. The bigger the backlog, the denser the drain and the sooner it hangs again. That is
the whole loop, and it is why every manual recovery so far has bought minutes rather than days:

| Restart | Backlog drained | Time to re-hang |
|---|---|---|
| 2026-09-09 11:13 | ~3,600 jobs | 10 minutes |
| 2026-09-15 08:46 | 324 entries | under 3 minutes |

### Detection gap

`JobQueueRepository.getInProgress()` has exactly one production caller,
`BootstrapBean.resetJobsInterruptedDuringPartitioning()`, which runs at startup. There is no
runtime check on how long an entry has been `IN_PROGRESS` and no metric on queue age.

Combined with the fact that the stall is per sink **and submitter**, so affected sinks keep
delivering other work, there was no signal available to anyone. Both occurrences were found by
inspection, not by alerting.

### Recovery hazard

A plain pod restart is **not** a safe recovery on its own. `BootstrapBean.resetJobsInterruptedDuringPartitioning()`
calls `jobSchedulerBean.ensureLastChunkIsScheduled(jobId)` before resetting the entry to
`WAITING`, and that method (`JobSchedulerBean.java:206`) is:

```java
int chunkId = Math.max(0, jobEntity.getNumberOfChunks() - 1);
ChunkEntity chunkEntity = entityManager.find(ChunkEntity.class, new ChunkEntity.Key(chunkId, jobId));
if (chunkEntity != null && !dependencyTrackingService.isScheduled(chunkEntity)) {
    scheduleChunk(chunkEntity, jobEntity);
}
```

`isScheduled` only tests whether the dependency tracking key is present, and every tracking key
for a completed job has been removed. There is no `getTimeOfCompletion()` guard on this path,
unlike `findMissingDependencies`, which does check it. So for an orphaned entry whose job already
finished, a restart re-schedules the job's last chunk and delivers it a second time.

The re-partitioning itself is safe. `partitionJobIntoChunksAndItems` resumes at
`chunkId = job.getNumberOfChunks()`, drains the already-read items and creates nothing. The
duplicate delivery comes solely from `ensureLastChunkIsScheduled`.

**Correct order: delete the orphaned queue row first, then restart.** That was done on
2026-09-15 and no duplicate delivery occurred.

---

## Correction to the July postmortem

The outstanding DBA action from July, `lock_timeout = '60s'` on the jobstore role, **would not
have caught any of these occurrences**. No stuck thread was waiting on a PostgreSQL lock. All were
waiting on a JVM monitor, which `lock_timeout` cannot observe.

It remains worth doing for the failure mode it was aimed at. It should not be recorded as
covering this one, and the July postmortem's action item list should be annotated accordingly.

---

## Fixes

Proposed, not yet shipped. Full detail in
`.claude/plans/2026-09-15-jobqueue-stall-eclipselink-cache-deadlock.md`.

Ranked after the lost-wakeup correction. Fix 1 alone closes the failure. The rest reduce
exposure or shorten detection.

1. **Bound the cache-key wait. Configuration only, no code change.** Set
   `eclipselink.concurrency.manager.waittime` to 5000 ms as a **JVM system property**.
   `ConcurrencyUtil.SINGLETON` reads it from `SystemProperties` at class-load and
   `setAcquireWaitTime` is never called anywhere in EclipseLink. A constant of the same name
   exists in `PersistenceUnitProperties`, but nothing reads it from `persistence.xml`, so
   configuring it there would silently do nothing.

   **This is now a complete fix, not a mitigation.** The previous version of this document assumed
   the bounded wait would end in a `ConcurrencyException`, be retried twice by
   `PARTITION_RETRY_POLICY`, and land in `abortJobDueToUnforeseenFailuresDuringPartitioning` after
   roughly 2.5 minutes, marking a completed job `FATAL` and leaving the entry orphaned. That
   reasoning belonged to the leak model, where the lock is never released. There is no leak. The
   lock is released within microseconds and only the notification is lost, so on timeout the
   thread re-enters the `while (toWaitOn != null)` loop, retries `acquireLockAndRelatedLocks`,
   finds the key free, and completes normally about 5 seconds late. No exception, no retry, no
   abort, no `FATAL` diagnostic, no orphaned entry. **Fix 1b is not needed and is withdrawn.**

   **Version: confirmed against the production binary.** `org.eclipse.persistence.core.jar` was
   copied out of the running container and inspected. `Bundle-Version:
   4.0.7.payara-p1v202509010857`. It is 4.0.7, but a **Payara-patched build** that carries the old
   untimed monitor wait rather than upstream 4.0.7's condition-based bounded one. Bytecode of
   `acquireLocksForClone`:

   ```
   161: monitorenter
   164: invokevirtual  CacheKey.isAcquired:()Z
   175: invokevirtual  ConcurrencyUtil.getAcquireWaitTime:()J
   178: invokevirtual  java/lang/Object.wait:(J)V
   231: monitorexit
   ```

   The LineNumberTable maps source line 184 to offsets 170 through 180, an exact match for
   `WriteLockManager.acquireLocksForClone(WriteLockManager.java:184)` in both dumps. Defaults in
   the same binary: `waittime` is `lconst_0`, `maxsleeptime` is `ldc2_w 40000L`, and
   `allow.interruptedexception` is `iconst_1`. So the fix is confirmed correct against the exact
   code that is running.

   **Upgrading is not an alternative.** We are already on 4.0.7. Upstream 4.0.7 fixed this path
   with a `ReentrantLock` condition bounded by `MAX_WAIT = 600000`, which is also the form
   `ConcurrencyManager.release` signals. Payara's patched build does not carry that form, so a
   newer Payara cannot be assumed to pick it back up. This must be reported to Payara.

2. **Take queue-entry removal off the hanging path. Still worth doing, no longer sufficient.**
   `JobQueueRepository.remove` is the only way an entry is cleared and it is where two of the
   three observed hangs occurred, because `entityManager.merge` reaches `acquireLocksForClone`.
   Replacing it with a bulk JPQL delete by primary key removes the clone entirely. The class
   already has this shape in `DELETE_BY_JOBID`.

   Note what it does not cover. In capture 1, `__ejb-thread-pool6` hung in
   `seizeHeadOfQueueIfWaiting` inside `getResultList`, which reaches the same wait by a different
   route. Any cascaded clone of `JobQueueEntity` can hang, so removing one call site narrows the
   exposure without closing it. Ship it with fix 1, not instead of it.

3. **Runtime watchdog.** A scheduled check over `getInProgress()` that reports entries stuck past
   a threshold, with a log line and a metric. Detection only. This is what turns the next
   occurrence from days into minutes, and given the three minute time-to-recur it will be
   exercised immediately.

4. **Take `JobQueueEntity` out of the shared cache.** `@Cacheable(false)`. Removes this entity's
   cache keys from the shared identity map. Under the lost-wakeup model this is a genuine
   reduction in contention rather than the narrowing-by-luck the leak model made it look like,
   since the whole failure needs two threads on one shared key. It still does not cover
   `RerunEntity`, which has the same non-LAZY relationship shape and the same seize and remove
   structure in `RerunsRepository`, and it does not help the `JobEntity` key that the cascade
   reaches for. Optional, and strictly after fix 1.

---

## Action Items

| # | Action | Owner | Status |
|---|---|---|---|
| 1 | ~~Establish the EclipseLink version.~~ **Done 2026-09-15:** `4.0.7.payara-p1v202509010857`, confirmed from the jar in the running container | | Closed |
| 2 | Set `eclipselink.concurrency.manager.waittime=5000` as a JVM option, and verify at runtime with `jinfo -sysprops` that it is actually applied | | Open |
| 3 | Pin `docker.payara.version`, currently `latest`. The JPA provider is a Payara-patched build whose behaviour differs from upstream of the same version number, and it can change with no commit | | Open |
| 3b | Report to Payara that `4.0.7.payara-p1v202509010857` pairs a `Condition`-based `ConcurrencyManager` with a monitor-based `WriteLockManager.acquireLocksForClone`, so `release` can never wake a waiter. Not a tuning issue, a hard bug | | Open |
| 3c | Pull the image digest history for the jobstore pod around 2026-09-01 to 2026-09-04 and establish when this Payara build was first deployed. That date is the onset of the whole incident family | | Open |
| 4 | Change `JobQueueRepository.remove` to a bulk delete by id, so entry removal cannot reach `acquireLocksForClone` | | Open |
| 5 | Delete the orphaned entry **before** restarting, then restart. Do not restart without applying action 2 in the same rollout, or the stall returns within minutes | | Open |
| 6 | Add the `IN_PROGRESS` watchdog with log plus metric, and wire an alert to it | | Open |
| 7 | Add a `getTimeOfCompletion()` guard to `ensureLastChunkIsScheduled` so restart recovery cannot re-deliver a completed job's last chunk | | Open |
| 8 | Add `@Cacheable(false)` to `JobQueueEntity`. Optional | | Open |
| 9 | Bound the prior-entry guard's failure mode so one wedged entry cannot stall a submitter indefinitely. Separate MR, touches the ordering guarantee | | Open |
| 10 | Annotate the July postmortem: `lock_timeout` does not cover the cache-key wait, which is a JVM monitor | | Open |
| 11 | ~~Identify the leaking acquire path.~~ **Closed 2026-09-16:** there is no leak. `release` signals `instanceLockCondition`, `acquireLocksForClone` waits on the CacheKey monitor, and no class in the jar calls `notify` | | Closed |
| 12 | Consider a `timeOfStateChange` column on `jobqueue` so stuck-entry age is measurable from the database rather than from process memory | | Open |
| 13 | Decide whether `RerunEntity` needs the same treatment as `JobQueueEntity`, or whether actions 2/3 are sufficient cover | | Open |

---

## Lessons Learned

**Two blocked threads are not evidence of a deadlock.** Capture 1 contained a blocked seize and a
blocked remove, which matched the July model closely enough to be persuasive. It was wrong. The
two threads own two different CacheKeys, which the first two readings of the dump never checked.
Look for the counterparty explicitly rather than infer it from the shape of the stacks, and print
the monitor addresses before calling anything a cycle.

**No counterparty is not evidence of a leak either.** Having ruled out a deadlock, the second
version concluded that the lock must be held by a thread that walked away, because that is the
only way a wait can outlive its holder if you assume the wait and the wake-up are paired. They
were not paired. The assumption that never got questioned was the cheapest one to check, and
checking it needed one `grep` over the jar.

**When an inference ends in "not established", that is a signal to go down a level.** The
previous version stated plainly that the leaking operation could not be identified and opened an
action item to hunt it with concurrency logging in a test environment. That hunt would have found
nothing, because there was nothing to find. An unexplainable residue in an otherwise tidy
explanation usually means the model is wrong, not that the evidence is missing.

**A fix that targets the wrong layer can look like it worked.** PR #294 and PR #295 were both
correct and both verified against the symptom available at the time. Neither touched the layer
this failure actually lives in. The July analysis named the EclipseLink cache-key lock explicitly
and still modelled it only as one half of a cross-system cycle.

**Library defaults can disable a library's own safety net.** EclipseLink ships a breaker for this
method, enabled by default, and a different default on the adjacent line makes it unreachable.
Reading the source of the exact version in use was what settled this, and nothing short of that
would have.

**A wait and a signal on the same object are not necessarily the same lock.** `synchronized (x)`
plus `x.wait()` and `x.instanceLockCondition.await()` name the same object and share nothing. This
is what a partial migration from intrinsic monitors to `java.util.concurrent` locks looks like
when it goes wrong, and the compiler has nothing to say about it. Anywhere both forms exist in one
class family, check that every wait site and every signal site agree on which one they use.

**A version number is not a version.** The build resolves EclipseLink 4.0.7 and the runtime is
EclipseLink 4.0.7, and the analysis was still nearly wrong, because the running artefact is
`4.0.7.payara-p1v202509010857` and its `WriteLockManager` does not match upstream 4.0.7's. Upstream
bounds this wait with a `ReentrantLock` condition, the same one `release` signals. Payara's patched
build of the same version number uses the old `Object.wait` on the monitor. Reading Maven's
resolved version, or the upstream source for that version, would have produced the wrong
conclusion in both directions: first that the flag was a no-op, then that upgrading was the fix.

What settled it was the stack frame form in the dump (`Object.wait` plus a monitor on the
CacheKey, which a `ReentrantLock` condition cannot produce) and then the bytecode of the jar
pulled out of the running container. For a bug that lives inside a library, get the artefact that
is actually loaded, and disassemble it rather than reading the source that ought to match it.

**A floating tag is an undeclared dependency change.** `docker.payara.version` is `latest`, so the
JPA provider can change under us with no commit, no review and no changelog entry. The onset of
this incident family is almost certainly an image pull, and we cannot date it from anything in the
repository.

**An untimed wait in a request path is a latent permanent outage.** Anywhere a request-serving
thread can block without a bound is a place where a transient event, contention, a leak or a lost
signal alike, becomes a permanent one. A timeout would have reduced this entire incident family to
a 5 second hiccup without anyone needing to know what the underlying defect was. The bound matters
more than the diagnosis.

**A restart that reproduces the failure is not a recovery.** Every manual recovery so far has
re-triggered the hang within minutes, because restarting is exactly what concentrates the backlog
onto the contended path. Measure time-to-recur after an intervention before recording it as
resolved.

**Head-of-line guards need a bound.** The prior-entry guard is correct for its purpose and
converts any unbounded stall into an unbounded, submitter-wide stall. Correctness under normal
operation and blast radius under failure are separate properties.

**Partial degradation is harder to detect than total failure.** July exhausted the thread pool and
halted all partitioning, and was caught in 55 minutes. These occurrences cost one or two threads
out of sixteen and the first ran for almost six days. The less severe failure was far more
expensive to detect.

---

## Unrelated observation

Between 08:47:06 and 08:48:01 on 2026-09-15, three jobs failed reading back PROCESSED chunks with
`IllegalArgumentException: item can not be null`:

```
Key{jobId=38304642, chunkId=63}
Key{jobId=38304755, chunkId=88}
Key{jobId=38304798, chunkId=98}
```

All three retried and completed by 08:54, so nothing is stuck. The shape suggests a read racing
an uncommitted item write during the burst drain. Recorded because it is a real error with a
plausible race behind it, not because it needs action now.

These are unrelated to the stall, and the timing invites the opposite conclusion, so it is worth
saying why. A rolled-back transaction in the middle of an EclipseLink unit of work is a plausible
way to leak a lock, and these rollbacks land 85 seconds before the hang in capture 2. But capture
1 has no `WARN` or `ERROR` of any kind between the 11:13:03 pod start and the 11:23:08 hang, so
the failure clearly does not need them. With the lost-wakeup mechanism established they are not
needed as an explanation either.

---

## Diagnostic artifacts

| File | Capture | Contents |
|---|---|---|
| `jobstore.thread_dump.20261509.txt` | 1, 07:18 | Full thread dump, 419 threads. Two threads in `WriteLockManager`. |
| `jobstore.db.state` | 1, 07:27 | `pg_stat_activity`, `pg_locks` on job tables, blocked/blocking pairs. |
| `jobqueue.db` | 1, 07:30 | `select * from jobqueue order by id asc`, 327 rows. |
| `jobstore.log` | 1, to 07:32 | Service log, 438 MB. |
| `jobstore.thread_dump.20261509.2.txt` | 2, 08:54 | Full thread dump. **One** thread in `WriteLockManager`, and the only thread in the JVM with any EclipseLink frame. |
| `jobstore.log.2` | 2, to 08:56 | Service log from the restart onward, 8.5 MB. |
| `org.eclipse.persistence.core.jar` | 2, 09:19 | The loaded JPA provider, copied out of the running container. `Bundle-Version: 4.0.7.payara-p1v202509010857`. This is the artefact the root cause was established from. |

Useful checks against a future capture:

```bash
# Are any threads parked in the EclipseLink lock manager?
grep -n 'WriteLockManager\|acquireLocksForClone' <thread-dump>

# Which thread does each hit belong to?
awk '/^"/{t=$1} /WriteLockManager/{print t" :: "$0}' <thread-dump>

# Is there a counterparty at all? If this lists only the blocked threads themselves, there is none.
awk '/^"/{t=$1} /org\.eclipse\.persistence/{print t}' <thread-dump> | sort -u

# Do the blocked threads share a CacheKey? Different addresses means independent victims, not a cycle.
awk '/^"/{t=$1} /WriteLockManager.acquireLocksForClone/{print t; getline; print "   "$0}' <thread-dump>

# Last activity of a suspect thread, which dates the hang
grep '__ejb-thread-poolN' <logfile> | tail -3
```

Checks against the loaded library, which is what actually settled this:

```bash
# Copy the jar out of the running pod first, then:
unzip -q org.eclipse.persistence.core.jar -d el && cd el

# Does anything in the library ever signal an intrinsic monitor?
grep -rl notify --include='*.class' .          # expect no output on the broken build

# Which primitive does release use?
javap -p -c org/eclipse/persistence/internal/helper/ConcurrencyManager.class \
  | grep -E 'Condition.signalAll|Object.notify|monitorenter'

# Which primitive does the wait site use?
javap -p -c org/eclipse/persistence/internal/helper/WriteLockManager.class \
  | grep -E 'monitorenter|Object.wait|Condition.await'
```

If `release` shows `Condition.signalAll` and `acquireLocksForClone` shows `monitorenter` plus
`Object.wait`, the build is affected.

The July postmortem's diagnostic procedures (thread dump from a running pod, the three PostgreSQL
queries) apply unchanged and are not repeated here. Note that the PostgreSQL queries will look
normal, as they did in capture 2, because no database lock is involved.

---

## Related Documents

- `docs/postmortem-2026-07-jobstore-jobqueue-stall.md` - the first two rounds, PR #294 and PR #295
- `.claude/plans/2026-09-15-jobqueue-stall-eclipselink-cache-deadlock.md` - implementation plan for the fixes above
- [EclipseLink caching overview](https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Basic_JPA_Development/Caching/Caching_Overview)
