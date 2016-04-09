--drop table IF EXISTS dependency_tracking;

create table dependencytracking (
    jobId    INTEGER NOT NULL,
    chunkId  INTEGER NOT NULL,
    sinkId   INTEGER NOT NULL,
    status   SMALLINT NOT NULL,
    waitingOn JSONB,
    blocking JSONB,
    matchKeys JSONB,
    PRIMARY KEY ( jobId, chunkId ),
    FOREIGN KEY ( jobid, chunkid ) REFERENCES chunk( jobid, id) on delete cascade
);

CREATE INDEX dependency_tracking_matchKeys on dependencytracking USING GIN (matchKeys jsonb_path_ops);




