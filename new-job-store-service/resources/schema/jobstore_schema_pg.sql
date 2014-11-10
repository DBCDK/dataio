CREATE TABLE flowcache (
    id                      SERIAL,
    checksum                TEXT UNIQUE NOT NULL,
    flow                    JSON NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE sinkcache (
    id                      SERIAL,
    checksum                TEXT UNIQUE NOT NULL,
    sink                    JSON NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE job (
    id                      SERIAL,
    numberOfChunks          INTEGER NOT NULL DEFAULT 0,
    numberOfItems           INTEGER NOT NULL DEFAULT 0,
    timeOfCreation          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timeOfCompletion        TIMESTAMP,
    timeOfLastModification  TIMESTAMP DEFAULT timeofday()::TIMESTAMP,
    specification           JSON NOT NULL,
    state                   JSON NOT NULL,
    flow                    INTEGER REFERENCES flowcache(id),
    sink                    INTEGER REFERENCES sinkcache(id),
    flowName                TEXT NOT NULL,
    sinkName                TEXT NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX job_timeOfCreation_index ON job(timeOfCreation);
CREATE INDEX job_timeOfLastModification_index ON job(timeOfLastModification);

CREATE TABLE chunk (
    id                      INTEGER NOT NULL,
    jobId                   INTEGER NOT NULL,
    numberOfItems           INTEGER NOT NULL DEFAULT 10,
    timeOfCreation          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timeOfCompletion        TIMESTAMP,
    timeOfLastModification  TIMESTAMP DEFAULT timeofday()::TIMESTAMP,
    sequenceAnalysisData    JSON NOT NULL,
    state                   JSON NOT NULL,
    PRIMARY KEY (jobId, id)
);
CREATE INDEX chunk_timeOfLastModification_index ON chunk(timeOfLastModification);

CREATE TABLE item (
    id                      SMALLINT NOT NULL,
    chunkId                 INTEGER NOT NULL,
    jobId                   INTEGER NOT NULL,
    timeOfCreation          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timeOfCompletion        TIMESTAMP,
    timeOfLastModification  TIMESTAMP DEFAULT timeofday()::TIMESTAMP,
    contents                JSON NOT NULL,
    state                   JSON NOT NULL,
    PRIMARY KEY (jobId, chunkId, id)
);
CREATE INDEX item_stateFailed_index ON item(jobId, chunkId, id) WHERE
       state->>'partitioning' = 'failed'
    OR state->>'processing' = 'failed'
    OR state->>'delivering' = 'failed';

CREATE OR REPLACE FUNCTION update_timeOfLastModification() RETURNS TRIGGER
    LANGUAGE plpgsql
    AS
    $$
    BEGIN
        NEW.timeOfLastModification = timeofday()::TIMESTAMP;
        RETURN NEW;
    END;
    $$;

CREATE TRIGGER job_timeOfLastModification_trigger
    BEFORE UPDATE
    ON job
    FOR EACH ROW
    EXECUTE PROCEDURE update_timeOfLastModification();

CREATE TRIGGER chunk_timeOfLastModification_trigger
    BEFORE UPDATE
    ON chunk
    FOR EACH ROW
    EXECUTE PROCEDURE update_timeOfLastModification();

CREATE TRIGGER item_timeOfLastModification_trigger
    BEFORE UPDATE
    ON item
    FOR EACH ROW
    EXECUTE PROCEDURE update_timeOfLastModification();
