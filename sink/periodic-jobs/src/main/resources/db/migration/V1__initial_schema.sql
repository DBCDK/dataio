CREATE TABLE datablock (
    id       SERIAL PRIMARY KEY,
    jobId    INTEGER NOT NULL,
    sortkey  TEXT NOT NULL,
    bytes    BYTEA NOT NULL
);
CREATE INDEX datablock_jobid_index ON datablock(jobid);
CREATE INDEX datablock_sortkey_index ON datablock(sortkey);

CREATE TABLE delivery (
    jobId   INTEGER PRIMARY KEY,
    config   JSONB
);
