# Postmortem: Job Queue Stall - Leaked EclipseLink Cache-Key Lock

**Service:** job-store-service
**Dates:** 2026-09-09 through 2026-09-15, recurring
**Severity:** P2 (no thread-pool exhaustion, isolated to one submitter at a time)
**Status:** Diagnosed, fixes proposed and not yet shipped. Recurred after manual recovery.

> **Revision note (2026-09-15).** The first version of this document described the failure as a
> two-thread deadlock, following the model in the July postmortem. A second capture taken after
> manual recovery disproved that. The failure is a **leaked cache-key lock**, not a deadlock. The
> root cause analysis below has been rewritten. The correction is recorded rather than quietly
> edited out, because the wrong model would have led to the wrong fix ranking.

---

## Summary

EJB pool threads park permanently inside EclipseLink's `WriteLockManager.acquireLocksForClone`,
waiting on a cache-key lock that no running thread holds. Each parked thread leaves one job queue
entry in `IN_PROGRESS` forever, and PR #295's prior-entry guard then blocks every later entry for
that sink and submitter.

Two occurrences are documented here. The first wedged two sinks for close to six days and three
days respectively. After manual recovery on 2026-09-15 the same failure reappeared within three
minutes of the restart, on the same sink.

The failure is permanent rather than slow because of a single library default:
`eclipselink.concurrency.manager.waittime` is `0`, which makes the cache-key wait untimed, which
in turn makes EclipseLink's own deadlock detector structurally unreachable.

It went unnoticed for days because nothing reads `JobQueueRepository.getInProgress()` at runtime,
and the affected sinks kept processing other submitters normally.

---

## Timeline

### First occurrence

| Time | Event |
|---|---|
| 2026-09-06 08:24:41 | Queue entry 36367474 (sink 13451, job 38304622) inserted, state `WAITING`. |
| 2026-09-09 11:23:01 | `__ejb-thread-pool11` seizes job 38304622 and begins partitioning. |
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

---

## Impact

| Metric | First occurrence | Recurrence |
|---|---|---|
| Undetected stall, sink 13451 | ~5 days 20 hours | ongoing at capture |
| Undetected stall, sink 4213 | ~3 days 18 hours | not affected |
| Queue entries backed up at capture | 326 `WAITING` plus 1 `IN_PROGRESS` | 2 `WAITING` plus 1 `IN_PROGRESS` |
| EJB pool threads lost | 2 of 16 | 1 of 16 |
| Thread-pool exhaustion | No | No |
| Other sinks affected | None | None |

Unlike July, neither occurrence exhausted the thread pool. Partitioning continued normally for
every other sink and submitter throughout. That is precisely why the first occurrence ran for
days without being noticed.

Recovery requires deleting the orphaned queue row and restarting the pod. Time to recur after
the restart was under three minutes.

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

All bottom out identically:

```
java.lang.Thread.State: WAITING (on object monitor)
    at java.lang.Object.wait0(java.base@21.0.2/Native Method)
    at java.lang.Object.wait(java.base@21.0.2/Object.java:366)
    at org.eclipse.persistence.internal.helper.WriteLockManager.acquireLocksForClone(WriteLockManager.java:184)
    - locked <0x00000006099c2b48> (a HardCacheWeakIdentityMap$ReferenceCacheKey)
```

In capture 1, `__ejb-thread-pool6` is PostgreSQL backend `1330884`: `idle in transaction`,
`wait_event = ClientRead`, `3 days 18:26:15`, running the seize query. PostgreSQL had finished
the `SELECT`, returned the rows, and was waiting for a client that was never coming back. The
`FOR UPDATE OF jq` row lock on entry 36400113 was held that entire time.

Note what is **not** present in either capture: no `BLOCKED` threads, no ungranted locks, and the
blocked/blocking pair query returns 0 rows. Neither thread was ever waiting on PostgreSQL.

### It is a leak, not a deadlock

Capture 2 is decisive. **`__ejb-thread-pool7` is the only thread in the entire JVM with any
`org.eclipse.persistence` frame on its stack.** There is no counterparty.

The lock it waits on is therefore held by a `ConcurrencyManager` whose `activeThread` points at
some thread that has long since left EclipseLink code. Same-thread re-entry cannot explain it,
because EclipseLink grants that:

```java
public boolean acquireReadLockNoWait() {
    instanceLock.lock();
    try {
        if ((this.activeThread == null) || (this.activeThread == Thread.currentThread())) {
            acquireReadLock();
            return true;
        } else {
            return false;
        }
    } finally {
        instanceLock.unlock();
    }
}
```

`__ejb-thread-pool7` performed both the seize (08:49:26.751) and the remove (08:49:26.842), so
any lock it took itself would be re-entrant and would not block it. `acquireLockAndRelatedLocks`
returned a non-null `toWaitOn`, which only happens when `activeThread` is set and belongs to a
**different** thread.

So the failure is: some earlier operation leaves `ConcurrencyManager.activeThread` set on a cache
key and never clears it, and every later clone or merge of that entity waits on it forever.
There is no cycle, no second victim, and nothing to break.

The identity of the leaking operation is **not established**. The thread dump cannot show it,
because `activeThread` is an ordinary field rather than a monitor, and by the time the symptom
appears the offending thread has returned to the pool. What is established is that no running
thread holds it and that the wait is unbounded.

This supersedes two earlier models:

- The July postmortem's cross-system deadlock, PostgreSQL row lock versus cache-key lock.
- The first version of this document's two-thread deadlock between seize and remove.

Both were consistent with capture 1, which happened to contain two blocked threads. Capture 2
contains one, and rules both out.

### Why the wait never ends

`WriteLockManager.acquireLocksForClone`, EclipseLink 4.0.x:

```java
// Since we know this one of those methods that can appear in the dead locks
// we threads frozen here forever inside of the wait that used to have no timeout
// we will now always check for how long the current thread is stuck in this while loop
ConcurrencyUtil.SINGLETON.determineIfReleaseDeferredLockAppearsToBeDeadLocked(
        toWaitOn, whileStartTimeMillis, lockManager, readLockManager, true);

synchronized (toWaitOn) {
    try {
        if (toWaitOn.isAcquired()) {
            toWaitOn.wait(ConcurrencyUtil.SINGLETON.getAcquireWaitTime());
        }
    } catch (InterruptedException ex) {
        // Ignore exception thread should continue.
    }
}
```

`ConcurrencyUtil` defaults:

```java
private static final long DEFAULT_ACQUIRE_WAIT_TIME = 0L;
private static final long DEFAULT_MAX_ALLOWED_SLEEP_TIME_MS = 40000L;
private static final boolean DEFAULT_INTERRUPTED_EXCEPTION_FIRED = true;
```

`getAcquireWaitTime()` returns `0`, so this is `Object.wait(0)`, an indefinite wait. That matches
the `Object.java:366` frame in both captures.

EclipseLink ships a breaker for exactly this situation, and the comment above shows the authors
knew about the hang. But the detector runs **before** the wait, once per iteration of the
`while (toWaitOn != null)` loop. With an untimed wait the thread never completes an iteration, so
the detector fires once at time zero, when `whileStartTimeMillis` is fresh and nothing looks
wrong, and then the thread parks forever. The escape hatch cannot be reached.

The same pattern appears in `ConcurrencyManager.acquireReadLock`, where
`instanceLockCondition.await(getAcquireWaitTime(), MILLISECONDS)` with a zero timeout is likewise
an unbounded wait, though the detector there is inside the loop and would recover once the wait
is bounded.

This is the whole reason a leaked lock becomes a permanent outage rather than a 40 second stall.
It is also why this single default is the only one of the proposed fixes that addresses the
failure as now understood.

### Why the July fixes did not prevent this

The July postmortem models the cycle as PostgreSQL row lock versus EclipseLink cache-key lock,
and both fixes act on the PostgreSQL side:

- PR #294 narrowed `FOR UPDATE` to `FOR UPDATE OF jq`.
- PR #295 added `jq.state = 'WAITING'`, the prior-entry guard, and `SKIP LOCKED`, making seize
  and remove row-disjoint.

Both are correct and both still hold. Neither is relevant here, because **no PostgreSQL lock is
part of this failure**. The row lock held by the seize in capture 1 is a consequence of the hang,
not a cause of it. A leaked in-process lock is invisible to any amount of SQL predicate work.

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

Which of the two cache keys is the leaked one is not determined. In capture 2 the job's own
`JobEntity` was demonstrably usable after the hang began, since
`addChunk: adding DELIVERED chunk 38341335/0` and the completion `TIMER` both succeeded at
08:49:31, five seconds after `__ejb-thread-pool7` blocked. That points at the `JobQueueEntity`
key rather than the `JobEntity` key, but it is one observation and not proof.

### The amplifier: one wedged entry stalls a whole submitter

PR #295's prior-entry guard is what turns a single parked thread into a multi-day queue stall:

```sql
AND NOT EXISTS (
  SELECT 1 FROM jobqueue prior INNER JOIN job pjob ON prior.jobid = pjob.id
  WHERE prior.sinkid = jq.sinkid AND prior.id < jq.id
    AND pjob.specification->>'submitterId' = job.specification->>'submitterId')
```

An older entry for the same sink and submitter excludes every later one, **in any state**. So:

- **Sink 13451, both occurrences**: the head entry is `IN_PROGRESS` forever, so every later entry
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
have caught either occurrence**. No stuck thread was waiting on a PostgreSQL lock. All were
waiting on a JVM monitor, which `lock_timeout` cannot observe.

It remains worth doing for the failure mode it was aimed at. It should not be recorded as
covering this one, and the July postmortem's action item list should be annotated accordingly.

---

## Fixes

Proposed, not yet shipped. Full detail in
`.claude/plans/2026-09-15-jobqueue-stall-eclipselink-cache-deadlock.md`.

Ranked after the leak correction. Only the first addresses the failure as now understood.

1. **Bound the cache-key wait. Configuration only, no code change.** Set
   `eclipselink.concurrency.manager.waittime` to 5000 ms as a **JVM system property**.
   `ConcurrencyUtil.SINGLETON` reads it from `SystemProperties` at class-load and
   `setAcquireWaitTime` is never called anywhere in EclipseLink. A constant of the same name
   exists in `PersistenceUnitProperties`, but nothing reads it from `persistence.xml`, so
   configuring it there would silently do nothing.

   A bounded wait recovers from a leaked lock exactly as well as from a cycle, which is why this
   survives the correction intact. `ConcurrencyException extends EclipseLinkException extends
   RuntimeException`, so `PARTITION_RETRY_POLICY` retries twice with a 10 s delay, and the
   exception reaches `catch (Throwable)` after roughly 2.5 minutes.

   **What it does not do.** The handler,
   `abortJobDueToUnforeseenFailuresDuringPartitioning`, then writes a `Diagnostic.Level.FATAL`
   onto a job that already completed successfully, and calls `remove()` again, which hits the same
   leaked lock. It uses the private `abortJob(int, String, Throwable)` overload
   (`PgJobStore.java:680`), which does **not** call `deleteByJobId`. So the queue entry is likely
   still orphaned and a good job is now marked aborted. Necessary, not sufficient. Pair with
   fix 1b.

   **Version: confirmed against the production binary.** `org.eclipse.persistence.core.jar` was
   copied out of the running container and inspected. `Bundle-Version:
   4.0.7.payara-p1v202509010857`. It is 4.0.7, but a **Payara-patched build** that carries the old
   untimed wait rather than upstream 4.0.7's bounded one. Bytecode of `acquireLocksForClone`:

   ```
   172: getstatic      ConcurrencyUtil.SINGLETON
   175: invokevirtual  ConcurrencyUtil.getAcquireWaitTime:()J
   178: invokevirtual  java/lang/Object.wait:(J)V
   ```

   The LineNumberTable maps source line 184 to offsets 170 through 180, an exact match for
   `WriteLockManager.acquireLocksForClone(WriteLockManager.java:184)` in both dumps. Defaults in
   the same binary: `waittime` is `lconst_0`, `maxsleeptime` is `ldc2_w 40000L`, and
   `allow.interruptedexception` is `iconst_1`. So the fix is confirmed correct against the exact
   code that is running.

   **Upgrading is not an alternative.** We are already on 4.0.7. Upstream 4.0.7 fixed this path
   with a `ReentrantLock` condition bounded by `MAX_WAIT = 600000`. Payara's patched build does
   not carry that form, so a newer Payara cannot be assumed to pick it back up. This is also worth
   reporting to Payara.

2. **Take queue-entry removal off the hanging path. Essential.** `JobQueueRepository.remove` is
   the only way an entry is cleared and it is exactly the code that hangs, because
   `entityManager.merge` reaches `acquireLocksForClone`. Replace it with a bulk JPQL delete by
   primary key, which does not clone through the identity map and therefore cannot reach that
   method. The class already has this shape in `DELETE_BY_JOBID`. This makes the success path of
   partitioning immune to the leak rather than merely bounding how long it fails for.

3. **Runtime watchdog.** A scheduled check over `getInProgress()` that reports entries stuck past
   a threshold, with a log line and a metric. Detection only. This is what turns the next
   occurrence from days into minutes, and given the three minute time-to-recur it will be
   exercised immediately.

4. **Take `JobQueueEntity` out of the shared cache.** `@Cacheable(false)`. Removes this entity's
   cache keys from the shared identity map, which should take the observed hang site out of play.
   Downgraded by the leak correction: against a leaked lock this is narrowing by luck rather than
   by mechanism, since it only helps if the leaked key is a `JobQueueEntity` key rather than a
   `JobEntity` key. It also does not cover `RerunEntity`, which has the same non-LAZY relationship
   shape and the same seize and remove structure in `RerunsRepository`. Optional.

---

## Action Items

| # | Action | Owner | Status |
|---|---|---|---|
| 1 | ~~Establish the EclipseLink version.~~ **Done 2026-09-15:** `4.0.7.payara-p1v202509010857`, confirmed from the jar in the running container | | Closed |
| 2 | Set `eclipselink.concurrency.manager.waittime=5000` as a JVM option, and verify at runtime with `jinfo -sysprops` that it is actually applied | | Open |
| 3 | Pin `docker.payara.version`, currently `latest`. The JPA provider is a Payara-patched build whose behaviour differs from upstream of the same version number, and it can change with no commit | | Open |
| 3b | Report the unbounded `Object.wait` in Payara's patched EclipseLink 4.0.7 upstream | | Open |
| 4 | Change `JobQueueRepository.remove` to a bulk delete by id, so entry removal cannot reach `acquireLocksForClone` | | Open |
| 5 | Delete orphaned entry 36404110 **before** restarting, then restart, ideally on the same restart that applies action 2 or 3 | | Open |
| 6 | Add the `IN_PROGRESS` watchdog with log plus metric, and wire an alert to it | | Open |
| 7 | Add a `getTimeOfCompletion()` guard to `ensureLastChunkIsScheduled` so restart recovery cannot re-deliver a completed job's last chunk | | Open |
| 8 | Add `@Cacheable(false)` to `JobQueueEntity`. Optional | | Open |
| 9 | Bound the prior-entry guard's failure mode so one wedged entry cannot stall a submitter indefinitely. Separate MR, touches the ordering guarantee | | Open |
| 10 | Annotate the July postmortem: `lock_timeout` does not cover the cache-key lock leak | | Open |
| 11 | Identify the leaking acquire path. Needs EclipseLink concurrency logging enabled in a non-production environment, since a thread dump cannot show `activeThread` | | Open |
| 12 | Consider a `timeOfStateChange` column on `jobqueue` so stuck-entry age is measurable from the database rather than from process memory | | Open |
| 13 | Decide whether `RerunEntity` needs the same treatment as `JobQueueEntity`, or whether actions 2/3 are sufficient cover | | Open |

---

## Lessons Learned

**Two blocked threads are not evidence of a deadlock.** Capture 1 contained a blocked seize and a
blocked remove, which matched the July model closely enough to be persuasive. It was wrong.
Capture 2 contained a single blocked thread and no counterparty, which is only consistent with a
leak. The lesson is to look for the counterparty explicitly rather than infer it from the shape
of the stacks, and to check whether the runtime even permits the cycle being hypothesised. Reading
`acquireReadLockNoWait` and finding same-thread re-entry granted is what settled it.

**A fix that targets the wrong layer can look like it worked.** PR #294 and PR #295 were both
correct and both verified against the symptom available at the time. Neither touched the layer
this failure actually lives in. The July analysis named the EclipseLink cache-key lock explicitly
and still modelled it only as one half of a cross-system cycle.

**Library defaults can disable a library's own safety net.** EclipseLink ships a breaker for this
method, enabled by default, and a different default on the adjacent line makes it unreachable.
Reading the source of the exact version in use was what settled this, and nothing short of that
would have.

**A version number is not a version.** The build resolves EclipseLink 4.0.7 and the runtime is
EclipseLink 4.0.7, and the analysis was still nearly wrong, because the running artefact is
`4.0.7.payara-p1v202509010857` and its `WriteLockManager` does not match upstream 4.0.7's. Upstream
bounds this wait with a `ReentrantLock` condition. Payara's patched build of the same version
number uses the old unbounded `Object.wait`. Reading Maven's resolved version, or the upstream
source for that version, would have produced the wrong conclusion in both directions: first that
the flag was a no-op, then that upgrading was the fix.

What settled it was the stack frame form in the dump (`Object.wait` plus a monitor on the
CacheKey, which a `ReentrantLock` condition cannot produce) and then the bytecode of the jar
pulled out of the running container. For a bug that lives inside a library, get the artefact that
is actually loaded.

**An untimed wait in a request path is a latent permanent outage.** Anywhere a request-serving
thread can block without a bound is a place where a transient event, contention or leak alike,
becomes a permanent one. The bound matters more than knowing which one it was.

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

Useful checks against a future capture:

```bash
# Are any threads parked in the EclipseLink lock manager?
grep -n 'WriteLockManager\|acquireLocksForClone' <thread-dump>

# Which thread does each hit belong to?
awk '/^"/{t=$1} /WriteLockManager/{print t" :: "$0}' <thread-dump>

# Critical question: is there a counterparty at all?
# If this lists only the blocked thread itself, it is a leak, not a deadlock.
awk '/^"/{t=$1} /org\.eclipse\.persistence/{print t}' <thread-dump> | sort -u

# Last activity of a suspect thread, which dates the hang
grep '__ejb-thread-poolN' <logfile> | tail -3
```

The July postmortem's diagnostic procedures (thread dump from a running pod, the three PostgreSQL
queries) apply unchanged and are not repeated here. Note that the PostgreSQL queries will look
normal in a pure leak, as they did in capture 2.

---

## Related Documents

- `docs/postmortem-2026-07-jobstore-jobqueue-stall.md` - the first two rounds, PR #294 and PR #295
- `.claude/plans/2026-09-15-jobqueue-stall-eclipselink-cache-deadlock.md` - implementation plan for the fixes above
- [EclipseLink caching overview](https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Basic_JPA_Development/Caching/Caching_Overview)
