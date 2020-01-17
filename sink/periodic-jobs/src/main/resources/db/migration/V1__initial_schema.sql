CREATE TABLE datablock (
    jobId        INTEGER NOT NULL,
    recordNumber INTEGER NOT NULL,
    sortkey      TEXT NOT NULL,
    bytes        BYTEA NOT NULL,
    PRIMARY KEY (jobId, recordNumber)
);
CREATE INDEX datablock_sortkey_index ON datablock(sortkey);

CREATE TABLE delivery (
    jobId   INTEGER PRIMARY KEY,
    config   JSONB
);
