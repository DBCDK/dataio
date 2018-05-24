CREATE TABLE block (
    jobId       INTEGER NOT NULL,
    chunkId     INTEGER NOT NULL,
    bytes       BYTEA NOT NULL,
    PRIMARY KEY (jobId, chunkId)
);
