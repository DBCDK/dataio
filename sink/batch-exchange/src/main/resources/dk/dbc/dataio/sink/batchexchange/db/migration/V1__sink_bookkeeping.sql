/*
The sink's own bookkeeping for the items it holds in the batch exchange.

Both tables live under this sink's own migration history table, kept apart from the migrations
of the batch-exchange-api artifact so that a version number added on either side cannot collide
with one added on the other.
*/

/*
staged_item: the items currently staged, one row per batch.

The five values needed to report an item's delivery have no home in the batch exchange schema
itself. batch.id is assigned by a sequence and entry belongs to the consumer system's contract,
which would leave batch.name as the only field this sink owns, holding all five packed into one
string. This table is the sink's own, so they are columns.

record_key is nullable because an item can arrive without one. NULLs compare as distinct in a
unique constraint, so keyless items never collide with each other, which is the existing rule
that an item with no record identity is never superseded.

The unique constraint makes "at most one version of a record staged at a time" an invariant
rather than a check. The broker's JMSXGroupID grouping already serializes the staging of one
record's versions, so it is not expected to fire. It guards the case the grouping does not
cover, where a false failure-detection positive produces two concurrent attempts at the same
work.
*/
CREATE TABLE staged_item (
    batch           INTEGER PRIMARY KEY REFERENCES batch (id) ON DELETE CASCADE,
    sink_id         BIGINT   NOT NULL,
    record_key      TEXT,
    job_id          INTEGER  NOT NULL,
    chunk_id        BIGINT   NOT NULL,
    item_id         SMALLINT NOT NULL,
    collapsed_count INTEGER  NOT NULL DEFAULT 0,
    collapsed_from  TEXT,
    UNIQUE (sink_id, record_key)
);

CREATE INDEX staged_item_job_index ON staged_item (job_id);

/*
held_item: an item held back because another version of the same record is already staged.

The consumer system applies claimed entries across a thread pool without serializing by record,
so two versions staged at once are applied in an undefined order. Rather than stage a second
version, the sink parks it here and BatchFinalizer stages it once the blocking batch completes.

One row per record: a newer version replaces the parked one and the displaced version is
reported as superseded. Versions collapse rather than queue, which is the rule the delivery
watermark already applies to versions that reached the target. collapsed_count and
collapsed_from record what was skipped, so a failure of the surviving version can be seen for
what it is. They are carried forward on each replacement and copied to staged_item on staging,
and they are bounded, holding a count and the oldest displaced version rather than a list.

record_key is NOT NULL here, unlike in staged_item: an item without one has no record identity,
so nothing can block it and it is never held.
*/
CREATE TABLE held_item (
    id              SERIAL PRIMARY KEY,
    sink_id         BIGINT   NOT NULL,
    record_key      TEXT     NOT NULL,
    job_id          INTEGER  NOT NULL,
    chunk_id        BIGINT   NOT NULL,
    item_id         SMALLINT NOT NULL,
    tracking_id     TEXT,
    priority        INTEGER  NOT NULL,
    payload         BYTEA    NOT NULL,
    collapsed_count INTEGER  NOT NULL DEFAULT 0,
    collapsed_from  TEXT,
    UNIQUE (sink_id, record_key)
);

CREATE INDEX held_item_job_index ON held_item (job_id);
