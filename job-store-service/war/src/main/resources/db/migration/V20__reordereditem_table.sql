CREATE TABLE reordereditem (
    jobId                   INTEGER,
    seqno                   INTEGER,
    sortOrder               INTEGER NOT NULL,
    chunkItem               JSONB NOT NULL,
    recordInfo              JSONB NOT NULL,
    PRIMARY KEY (jobId, seqno)
);

CREATE INDEX reordereditem_sortOrder_index ON reordereditem(sortOrder);
