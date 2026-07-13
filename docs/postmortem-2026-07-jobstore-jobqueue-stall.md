# Postmortem: Job Queue Stall — Cross-System Deadlock

**Service:** job-store-service  
**Dates:** 2026-07-09 through 2026-07-10  
**Severity:** P1  
**Status:** Resolved (PR #294, PR #295) — `lock_timeout` DBA action outstanding  

---

## Summary

On 2026-07-09, the job-store-service suffered a queue stall caused by a cross-system deadlock
between PostgreSQL row locks and EclipseLink in-memory cache locks — permanent thread-pool
exhaustion with no self-healing path and no timeout on either side. The deadlock was latent in a
9-year-old SQL query but only triggered when burst traffic from a harvester delivery compressed
the race window to match the seize interval (~11 ms). Two successive code fixes were required:
PR #294 moved the deadlock from the `job` row to the `jobqueue` row; PR #295 closed the root
cause by making seize and remove row-disjoint at the database level. One DBA action —
`lock_timeout = '60s'` on the jobstore role — remains outstanding.

---

## Timeline

| Time                | Event                                                                                                                                                                                                                             |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 2026-07-09 09:48:57 | **Stall begins (Round 1 deadlock).** A harvester delivers a batch of small ADDI_MARC_XML jobs to sink 4213. Rapid-fire re-trigger chain produces ~90 seizes/second. First 4 IN_PROGRESS entries timestamped 09:48:57–59.          |
| 09:48:57–09:49:01   | **Burst:** 94 → 97 → 87 → 90 → 62 seizes in five consecutive seconds. Race window hit repeatedly; seize queries acquire the `job` row lock on jobs being actively partitioned.                                                    |
| 10:06:27            | Second burst hits sinks 3217 and 4213, adding 4 more deadlocked pairs. **Total: 8 stuck IN_PROGRESS entries, all 16 EJB pool threads consumed.** Partitioning halts for all sinks.                                                |
| 10:43               | **Stall detected.** 689+ WAITING entries accumulated. Thread dump and live `pg_locks` / `pg_stat_activity` capture taken while stall is active.                                                                                   |
| 12:39               | **PR #294 deployed.** `FOR UPDATE OF jq` scopes the row lock to the `jobqueue` row only. Round 1 cycle cannot form.                                                                                                               |
| ~13:40              | **Round 2 deadlock.** PR #294 moved the overlap from `job` rows to `jobqueue` rows. 4 new deadlocked pairs: seize (holds `jobqueue` row lock) vs. remove/DELETE (holds EclipseLink cache lock). Identical cross-system structure. |
| 2026-07-10 06:31    | **PR #295 deployed.** `AND jq.state = 'WAITING'`, prior-entry guard (`NOT EXISTS`), and `SKIP LOCKED`. Seize only locks WAITING rows; remove only deletes IN_PROGRESS rows. Row-disjoint — cycle cannot form.                     |

---

## Impact

| Metric                              | Value           |
|-------------------------------------|-----------------|
| Undetected stall duration (Round 1) | ~55 minutes     |
| WAITING entries at detection        | 689+            |
| EJB pool threads exhausted          | 16/16 (Round 1) |
| Affected sinks                      | 4213, 3217      |

Thread-pool exhaustion in Round 1 halted partitioning for **all** sinks, not only those with
stuck entries. Recovery in both rounds required a pod restart, which triggers
`BootstrapBean.resetJobsInterruptedDuringPartitioning()` to reset stuck IN_PROGRESS entries to
WAITING.

---

## Root Cause Analysis

### The cross-system deadlock family

The stall is caused by a deadlock spanning two independent locking systems that have no
visibility into each other:

1. **PostgreSQL row locks** — held by `FOR UPDATE` while EclipseLink materialises the result set.
2. **EclipseLink cache-key WriteLocks** — held during merge/remove before the SQL DELETE runs.

PostgreSQL's deadlock detector sees the seize backend as an idle client; the JVM has no view of
the row-lock wait. Neither side detects a cycle. The pairs hang permanently with no timeout on
either side until a pod restart.

Two rounds occurred with identical structure but on different rows. Both trace to a single root:
the seize query had no `jq.state = 'WAITING'` filter. Under READ COMMITTED, PostgreSQL's
EvalPlanQual recheck re-evaluates `WHERE` predicates against the newly committed row version
after waiting on a concurrent lock. Without a state filter, a row that just transitioned to
`IN_PROGRESS` can still pass all other predicates (`sinkId` matches; the `NOT IN` subquery ran
against the stale snapshot in which the submitter was not yet excluded) — so `FOR UPDATE` locks
it and hands it to EclipseLink for cloning.

### The EclipseLink cache-key lock

EclipseLink maintains a [shared object cache](https://wiki.eclipse.org/EclipseLink/UserGuide/JPA/Basic_JPA_Development/Caching/Caching_Overview) —
an in-process [identity map](https://wiki.eclipse.org/Introduction_to_Cache_(ELUG)) that keeps
a canonical entity instance per primary key, shared across threads in the same JVM. When a query
returns rows, EclipseLink does not pass the raw JDBC result directly to the caller; it first
*materialises* each returned row into the cache. The call path is:

```
getResultList()
  → ReadAllQuery.registerResultInUnitOfWork()
    → cloneAndRegisterObject()
      → WriteLockManager.acquireLocksForClone()   ← acquires the cache-key WriteLock
```

For each entity about to be cloned, EclipseLink acquires an exclusive **cache-key WriteLock**
on that entity's identity-map entry. This is a pure Java lock (no database involvement). It is
held from the start of cloning until the enclosing [unit-of-work](https://wiki.eclipse.org/Introduction_to_EclipseLink_Transactions_(ELUG))
transaction commits or rolls back, to prevent two threads from concurrently updating the cached
representation of the same entity.

The critical consequence for this deadlock: the cache-key lock is acquired *inside*
`getResultList()`, while the PostgreSQL row lock from `FOR UPDATE` is still open. Both locks are
held simultaneously until the seize's `REQUIRES_NEW` transaction ends.

On the remove side, EclipseLink acquires the cache-key lock as part of `merge()`/`remove()`
processing, *before* generating the `DELETE` SQL statement.

This means the two operations acquire the two lock types in **opposite order**:

| Operation | First lock acquired | Second lock acquired |
|-----------|--------------------|--------------------|
| Seize     | PostgreSQL row lock (FOR UPDATE) | EclipseLink cache-key WriteLock (during getResultList) |
| Remove    | EclipseLink cache-key WriteLock (before DELETE) | PostgreSQL row lock (the DELETE itself) |

Opposite acquisition order on a shared resource is the textbook precondition for a deadlock.

### Round 1: Seize vs. Chunk Creation (job row)

The unqualified `FOR UPDATE` in the seize query locked rows from all joined tables, including
`job`. Under the EPQ race described above, a seize could acquire a tuple lock on the `job` row
of a job being actively partitioned. `createChunkEntity` (`PgJobStoreRepository.java:438`) issues
`SELECT … FOR UPDATE` on the same row via `PESSIMISTIC_WRITE`.

```
Thread S (Seize)                          Thread K (Chunk Creation)
────────────────────────────────────      ────────────────────────────────────
HOLDS  PG row lock on job row J           HOLDS  EclipseLink WriteLock on JobEntity J
       (unqualified FOR UPDATE)                  (PESSIMISTIC_WRITE find)
NEEDS  EclipseLink WriteLock on J         NEEDS  PG row lock on job row J
       (to clone result into UoW)                (SELECT … FOR UPDATE)

PostgreSQL sees Thread S as "idle in transaction" — JVM cannot observe the row-lock wait.
```

**Fix (PR #294):** `FOR UPDATE` → `FOR UPDATE OF jq`. The seize only needed to lock the
`jobqueue` row; restricting the lock removes the overlap with `createChunkEntity`'s lock target.

> **Note:** PR #294 was necessary but incomplete. The underlying race (no state filter)
> remained, and the deadlock reappeared on the `jobqueue` row ~1 hour after deployment.

### Round 2: Seize vs. Remove (jobqueue row)

After PR #294, the seize query locked only the `jobqueue` row. The same EPQ race now collided
with the remove thread deleting that same row on the success path of partitioning
(`PgJobStore.java:269`). EclipseLink acquires the cache-key lock on the entity before issuing
the DELETE SQL; the seize acquires the PostgreSQL row lock before EclipseLink can clone the
entity.

```
Thread S (Seize)                          Thread R (Remove, post-partition)
────────────────────────────────────      ────────────────────────────────────
HOLDS  PG row lock on jobqueue row J      HOLDS  EclipseLink WriteLock on JobQueueEntity J
       (FOR UPDATE OF jq)                        (acquired before issuing DELETE)
NEEDS  EclipseLink WriteLock on J         NEEDS  PG row lock on jobqueue row J
       (to clone query result)                   (to execute DELETE)

Remove only deletes IN_PROGRESS rows — seize had no state filter — same race, different row.
```

Round 2 produced 4 stuck pairs (vs. 8 in Round 1). The remaining 8 EJB pool threads were idle —
this round was not thread-pool exhaustion; the stall came from 4 permanently held IN_PROGRESS
entries and their broken re-trigger chains.

---

## Why Now? Nine Years of Silence

The unqualified `FOR UPDATE` dates from June 2017 (commit `b8f560fb8`, "Fixed bug where job
queue entries were not locked when attempting to seize"). No partitioning code changed in 2026.
What changed is traffic shape.

The deadlock requires two seize queries to race on the same job within a millisecond-wide window.
At normal seize rates (10–300/hour) this window essentially never closes. At 09:00 on July 9 the
rate was **1,577 in one hour**, and the burst was dense:

```
09:48:57   94 seizes
09:48:58   97 seizes
09:48:59   87 seizes
09:49:00   90 seizes
09:49:01   62 seizes
```

One seize every ~11 ms is the same order of magnitude as the race window. The trigger probability
scales roughly with the **square** of the seize rate on a single sink. A harvester delivered a
backlog of small ADDI_MARC_XML jobs to sink 4213 in one batch; each completed partitioning
immediately re-triggers the next seize (`PgJobStore.java:281`), creating a rapid-fire chain at
whatever rate small jobs complete.

**Contributing factor:** Since the "Clusterfying jobstore" change (March 2024, commit
`adc818f77`), all partitioning runs on the Hazelcast master node. The EclipseLink cache-key lock
is JVM-local — the deadlock requires both threads in the same JVM, a condition the clustering
change permanently satisfied.

**What was ruled out:** Live `pg_locks` queries taken during the stall show chunk backends in
`wait_event = Lock / transactionid` — a real PostgreSQL lock wait, not a dead TCP socket or a
JTA timeout (set to 5 days).

### Why this deadlock family is rare in practice

Five preconditions must hold simultaneously. In normal operation most of them are absent.

**1. The EPQ race window is narrow.**
The seize can only be handed an IN_PROGRESS row if it executes its `FOR UPDATE` within
milliseconds of another seize's commit — the window during which the EPQ recheck runs against a
stale snapshot. At the normal seize rate (seconds between seizes on a given sink) this window
essentially never closes. The burst regime, where seizes arrive every ~11 ms, is what makes it
routine.

**2. The cache-key lock and the row lock must be held concurrently by the same thread.**
For most `SELECT FOR UPDATE` patterns, the Java code acquires the row lock, reads a lightweight
result, and commits before anything else contends on that row. The deadlock requires the ORM to
be mid-materialisation — cloning a `JobEntity` with its JSONB specification blob — while the row
lock is still open. This overlap exists because EclipseLink holds both locks simultaneously
(row lock from `FOR UPDATE` stays open; cache-key lock is acquired inside `getResultList()`
before the transaction ends). A thinner result or a pattern that committed before cloning would
not create the overlap.

**3. Both sides must be in the same JVM.**
EclipseLink's cache-key lock is an in-process Java monitor — threads on different JVMs have
entirely separate identity maps and never contend on the same cache key. Before the clustering
change (March 2024), seizes could run on any node, making shared-cache contention impossible.
Concentrating all partitioning on the Hazelcast master made it a permanent precondition.

**4. Most ORM-managed `SELECT FOR UPDATE` patterns acquire locks in the same order on both
sides.**
A typical read-modify-commit sequence (seize → UPDATE → commit) acquires cache-key lock and
row lock in the same order on both sides: row lock via `FOR UPDATE`, then cache-key lock during
materialisation, before the update. No cycle can form from matching acquisition order. The
deadlock here requires two *different* operations — SELECT FOR UPDATE and DELETE — to acquire the
same two lock types in opposite orders. That only happens when one operation holds a row lock
while the ORM materialises the result (unusual: most selects don't hold row locks during
materialisation) and the other holds a cache-key lock while waiting to acquire the row lock for a
DELETE (less unusual, but only a problem when the first condition is also true).

**5. The remove side holds the cache-key lock only during a brief commit window.**
In normal throughput the remove runs well after its matching seize has committed and released the
row lock, so there is no row-level contention. The deadlock window only opens when a seize
happens to execute its `FOR UPDATE` during the few milliseconds between the remove's
`acquireLocksForClone` call and its `DELETE` reaching PostgreSQL.

---

## Fixes Applied

### PR #294 — FOR UPDATE OF jq (partial fix)

**File:** `JobQueueEntity.java`  
**Status:** Merged 2026-07-09 12:39

```diff
-ORDER BY jq.id ASC LIMIT 1 FOR UPDATE;
+ORDER BY jq.id ASC LIMIT 1 FOR UPDATE OF jq;
```

Restricts the row lock to the `jobqueue` row. Removes the overlap with `createChunkEntity`'s
`job` row lock. Addresses Round 1 but left the EPQ race intact.

---

### PR #295 — Three cooperating predicates (comprehensive fix)

**Files:** `JobQueueEntity.java`, `JobQueueRepository.java`  
**Status:** Merged 2026-07-10 06:31

```sql
SELECT jq.* FROM jobqueue jq
INNER JOIN job ON jq.jobid = job.id
WHERE jq.sinkId = ?sinkId
  -- (1) Deadlock fix: EPQ re-checks this predicate against the committed row version,
  --     so IN_PROGRESS rows are rejected before FOR UPDATE acquires the lock.
  AND jq.state = 'WAITING'
  -- (2) Ordering guarantee: excludes any row with an older same-submitter entry
  --     regardless of that entry's state — snapshot-stable.
  AND NOT EXISTS (
      SELECT 1 FROM jobqueue prior
      INNER JOIN job pjob ON prior.jobid = pjob.id
      WHERE prior.sinkid = jq.sinkid
        AND prior.id < jq.id
        AND pjob.specification->>'submitterId' = job.specification->>'submitterId')
  -- (3) Belt-and-braces: logically redundant with (1)+(2), kept as defence-in-depth.
  AND job.specification->>'submitterId' NOT IN (
      SELECT specification->>'submitterId' FROM jobqueue jq_join
      INNER JOIN job ON jq_join.jobid = job.id
      WHERE jq_join.state = 'IN_PROGRESS' AND jq_join.sinkId = ?sinkId)
ORDER BY jq.id ASC LIMIT 1
FOR UPDATE OF jq SKIP LOCKED;
```

**Why `AND state='WAITING'` alone is wrong:** Under `LIMIT 1`, when EPQ rejects a candidate the
scan steps to the *next* qualifying row. With only the state filter, a seize stepping over a
locked/IN_PROGRESS head can land on a same-submitter later entry whose submitter still appears
free in the stale snapshot — partitioning two jobs from the same submitter concurrently. The
`NOT EXISTS` prior-entry guard closes this: it asks whether any *older* entry exists for this
submitter regardless of state, which is invariant to snapshot age. The same flaw applies to
`SKIP LOCKED` used alone.

**Why `SKIP LOCKED` is safe here:** Once the prior-entry guard is in place, skipping a locked
head row can only move to a *different* submitter's head. `SKIP LOCKED` is then a performance
win: concurrent seizes no longer queue behind the lock only to be rejected in Java.

---

### Backstop — lock_timeout (outstanding)

**Action:** DBA sets `lock_timeout = '60s'` on the jobstore role.  
**Status:** Pending

```sql
ALTER ROLE <jobstore_user> IN DATABASE <jobstore_db> SET lock_timeout = '60s';
-- Recycle pool connections or restart pods after applying.
```

The query fix removes the known deadlock family but cannot enumerate every future variant. A
server-side `lock_timeout` caps any stall at ~60 s: the waiting backend is cancelled, the
exception propagates into `abortJobDueToUnforeseenFailuresDuringPartitioning`, the IN_PROGRESS
entry is cleared, and the queue re-triggers.

**Do not use** `idle_in_transaction_session_timeout` — the main partitioning transaction
legitimately sits idle while fetching large data files over HTTP.  
**Do not use** `statement_timeout` — this service has legitimately slow statements (job exports,
purges).  
`lock_timeout` only fires on lock waits, which in a healthy system are milliseconds.

> **Note on JDBC socket timeouts:** URL parameters (`?socketTimeout=60`) appended to
> `JOBSTORE_DB_URL` are discarded by the `payara6-micro` base-image URL splitter and never reach
> the driver. Delivery as pool properties is an open investigation item (see Action Items).

---

## Action Items

| Owner         | Item                                                                                                                                                                                                                                                                                                                                                                                                                                         |
|---------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **DBA / Ops** | Apply `ALTER ROLE … SET lock_timeout = '60s'` on the jobstore role in production and recycle pool connections. This is the only outstanding fix from this incident.                                                                                                                                                                                                                                                                          |
| **Dev**       | Add integration tests to `AbstractJobStoreIT`: (a) ordering guarantee under concurrent seize — a same-submitter later entry must never be returned while an older entry exists; (b) deadlock precondition removed — IN_PROGRESS head plus WAITING same-submitter entry returns empty; (c) cross-submitter head is still returned when the head of queue is locked. See `docs/jobqueue-stall-fix-2.md` §Testing for full test specifications. |
| **Dev**       | Add alerting on "unexpected exception caught while partitioning job" log events. A `lock_timeout` firing is an incident signal, not expected operation — it must be visible and investigated.                                                                                                                                                                                                                                                |
| **Dev**       | Investigate delivering JDBC `socketTimeout` / `connectTimeout` as Payara pool properties (`property.socketTimeout=60`, `property.connectTimeout=10`). URL parameters are discarded by the base-image launcher. This covers the network black-hole scenario not addressed by `lock_timeout`.                                                                                                                                                  |

---

## Lessons Learned

**Cross-system deadlocks are undetectable by either system alone.** PostgreSQL and the JVM each
observe half the cycle. The observable symptom — stuck IN_PROGRESS, retries=0, cured by pod
restart — is identical to several other historical stalls. Diagnosis required a simultaneous
thread dump *and* a live `pg_locks` / `pg_stat_activity` capture; either alone is insufficient.

**A partial fix that moves a bug is not the same as closing a root cause.** PR #294 was correct
for the Round 1 mechanism but left the underlying EPQ race intact. Deploying to production before
confirming the race itself was closed was the direct cause of Round 2.

**Correctness invariants must be enforced in the query, not in Java.** The state guard at
`JobQueueRepository.java:86` was the only line of defence for the ordering guarantee during the
deadlock — and it was unreachable because the deadlock prevented the method from returning.

**Burst-sensitivity scales with the square of the seize rate.** The code ran for nine years
because the race window is milliseconds wide and the seize rate was low. Any future change
increasing seize frequency on a single sink — new harvester types, shorter re-trigger delay,
larger batches — re-opens the window. The query fix makes this moot, but the burst pattern is a
canary worth monitoring.

**Absent timeouts are a system design decision, not an oversight.** The EJB transaction timeout
was 5 days; no PostgreSQL lock wait timeout was configured. Any code path where two threads can
each hold a resource the other needs must have a timeout on at least one side.

---

## Diagnostic Procedures

The cross-system deadlock is only diagnosable with evidence from both the JVM and PostgreSQL
captured at the same time, while the stall is still active. A pod restart clears the state. Do
not restart until both captures are complete.

### Obtaining a thread dump from a running pod

A JVM thread dump prints the stack trace and lock state of every thread to stdout.

Exec into the running jobstore container and run this for determining the PID if the payara process and create
the thread dump file:

```bash
jps -l
jstack -l <PID> > jobstore_thread_dump.txt
```

Use `kubectl cp` to copy the file to your local machine.

**What to look for in the dump for this deadlock:**

- Threads blocked in `WriteLockManager.acquireLocksForClone` inside a `seizeHeadOfQueueIfWaiting`
  call (`JobQueueRepository.java:84`, inside `getResultList()`) — these are the seize side. They
  hold an open PostgreSQL transaction but appear idle from the database's perspective.
- Threads blocked in `processResults` (JDBC result processing) inside `createChunkEntity` or the
  delete path of `partitionNextJobForSinkIfAvailable` — these are the chunk/remove side. They
  are the backends PostgreSQL shows as `active / Lock`.
- The number of pairs matches the number of stuck IN_PROGRESS entries in the `jobqueue` table.

---

### PostgreSQL diagnostic queries

Run these against the jobstore database while the stall is active. They reveal the database half
of the cycle that the thread dump cannot show.

#### 1. Active backends and what they are waiting on

```sql
SELECT pid, state, wait_event_type, wait_event,
       left(query, 120) AS query,
       now() - query_start AS query_duration
FROM pg_stat_activity
WHERE datname = current_database()
  AND pid <> pg_backend_pid()
ORDER BY query_start;
```

| Column                           | What it tells you                                                                                                                                                                                                                                                       |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pid`                            | PostgreSQL backend process ID — joins to `pg_locks`                                                                                                                                                                                                                     |
| `state`                          | `active` = executing SQL; `idle in transaction` = inside a transaction but waiting for the Java client to send its next command — the seize threads appear here because they are blocked in Java (EclipseLink `WriteLockManager`) while their transaction is still open |
| `wait_event_type` / `wait_event` | `Lock / transactionid` = waiting to acquire a lock held by another transaction (the chunk or remove side); `Client / ClientRead` = waiting for the Java client to send SQL (the seize side, idle in transaction)                                                        |
| `query`                          | The last SQL statement this backend executed. For the seize side this will be the `SELECT … FOR UPDATE` query; for the chunk/remove side it will be either `SELECT … FOR UPDATE` on `job` or `DELETE FROM jobqueue`                                                     |
| `query_duration`                 | How long since that statement started. Stuck pairs are matched by similar `query_start` times — the seize backend is typically 10–60 ms older than its paired chunk/remove backend                                                                                      |

**Signature of this deadlock:** you will see two groups of backends with similar `query_start`
timestamps — one group in `idle in transaction / ClientRead` (the seize side) and one in
`active / Lock` (the chunk/remove side).

---

#### 2. Lock inventory on job-related tables

```sql
SELECT l.pid, l.granted, l.locktype, l.relation::regclass, l.mode,
       a.state, a.wait_event_type, a.wait_event,
       now() - a.query_start AS duration
FROM pg_locks l
JOIN pg_stat_activity a ON l.pid = a.pid
WHERE l.relation::regclass::text LIKE '%job%'
   OR l.locktype = 'transactionid'
ORDER BY l.granted, duration DESC;
```

| Column               | What it tells you                                                                                                                                                                                                                                                                                                |
|----------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `granted`            | `true` = the lock is currently held; `false` = waiting to acquire it                                                                                                                                                                                                                                             |
| `locktype`           | `relation` = a table-level lock; `tuple` = a row-level lock on a specific heap tuple; `transactionid` = waiting for a specific transaction to commit (this is the EPQ recheck wait — PostgreSQL holds the candidate row locked until the concurrent transaction commits, then re-evaluates the WHERE predicates) |
| `relation::regclass` | The table name. In Round 1 the contested relation was `job`; in Round 2 it was `jobqueue`                                                                                                                                                                                                                        |
| `mode`               | `RowShareLock` is taken by `FOR UPDATE` on joined tables (Round 1 symptom — the seize held `RowShareLock` on `job` even though it only needed `jobqueue`). `ExclusiveLock` (tuple level) is the `FOR UPDATE OF jq` lock on `jobqueue`                                                                            |

**Signature:** rows with `locktype = 'transactionid'` and `granted = false` show the chunk/remove
backends waiting for the seize transactions to commit. Each such row has a matching
`locktype = 'transactionid'` row with `granted = true` for the seize transaction. The durations
of the `granted = false` rows are the age of the deadlock.

---

#### 3. Blocked/blocking pairs

```sql
SELECT
    bl.pid             AS blocked_pid,
    ba.state           AS blocked_state,
    ba.wait_event_type,
    ba.wait_event,
    left(ba.query, 120) AS blocked_query,
    now() - ba.query_start AS blocked_duration,
    kl.pid             AS blocking_pid,
    left(ka.query, 120) AS blocking_query
FROM pg_locks bl
JOIN pg_stat_activity ba ON bl.pid = ba.pid
JOIN pg_locks kl
    ON kl.transactionid = bl.transactionid AND kl.pid <> bl.pid
JOIN pg_stat_activity ka ON kl.pid = ka.pid
WHERE NOT bl.granted;
```

| Column                            | What it tells you                                                                                                                                 |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `blocked_pid` / `blocked_query`   | The backend that is waiting for a lock — the chunk or remove side. Its query will be a `SELECT … FOR UPDATE` on `job` or a `DELETE FROM jobqueue` |
| `blocked_state` / `wait_event`    | `active / Lock` — actively blocked inside PostgreSQL's lock manager                                                                               |
| `blocked_duration`                | How long this backend has been waiting. In this deadlock, durations grow indefinitely                                                             |
| `blocking_pid` / `blocking_query` | The backend holding the lock the blocked one needs — the seize side. Its query will be the `SELECT … FOR UPDATE OF jq` seize query                |

**Important limitation:** this query shows only the PostgreSQL half of the deadlock. The
`blocking_pid` backend appears to be simply waiting for the Java client — PostgreSQL cannot see
that the seize thread is blocked inside `WriteLockManager.acquireLocksForClone`. This is why
PostgreSQL's own deadlock detector never fires: from its perspective, the blocking backend is
just idle, not in a lock wait. The thread dump is required to see the Java half.

---

## Related Documents

- PR #294 — `jobqueue-seize-deadlock`
- PR #295 — `jobqueue-seize-remove-deadlock`
