CREATE TABLE entitycache (
    id                      SERIAL,
    checksum                TEXT UNIQUE NOT NULL,
    entity                  JSON NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE job (
    id                      SERIAL,
    EOJ                     BOOLEAN NOT NULL DEFAULT TRUE,
    partNumber              INTEGER NOT NULL DEFAULT 0,
    numberOfChunks          INTEGER NOT NULL DEFAULT 0,
    numberOfItems           INTEGER NOT NULL DEFAULT 0,
    timeOfCreation          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timeOfCompletion        TIMESTAMP,
    timeOfLastModification  TIMESTAMP DEFAULT timeofday()::TIMESTAMP,
    specification           JSON NOT NULL,
    state                   JSON NOT NULL,
    flow                    INTEGER REFERENCES entitycache(id),
    sink                    INTEGER REFERENCES entitycache(id),
    flowName                TEXT NOT NULL,
    sinkName                TEXT NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX job_timeOfCreation_index ON job(timeOfCreation);
CREATE INDEX job_timeOfLastModification_index ON job(timeOfLastModification);

CREATE TABLE chunk (
    id                      INTEGER NOT NULL,
    jobId                   INTEGER NOT NULL,
    dataFileId              TEXT NOT NULL,
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
    state                   JSON NOT NULL,
    partitioningOutcome     JSON,
    processingOutcome       JSON,
    deliveringOutcome       JSON,
    PRIMARY KEY (jobId, chunkId, id)
);
CREATE INDEX item_stateFailed_index ON item(jobId, chunkId, id) WHERE
       state->>'partitioning' = 'failed'
    OR state->>'processing' = 'failed'
    OR state->>'delivering' = 'failed';

CREATE OR REPLACE FUNCTION set_entitycache(the_checksum TEXT, the_entity JSON) RETURNS INTEGER
    LANGUAGE plpgsql
    AS
    $$
    DECLARE
      the_id INTEGER;
    BEGIN
      LOOP
        UPDATE entitycache SET checksum=the_checksum WHERE checksum=the_checksum RETURNING id INTO the_id;
        IF FOUND THEN
          RETURN the_id;
        END IF;
        -- not found, try inserting instead and check exception in case of race condition
        BEGIN
          INSERT INTO entitycache (checksum, entity) VALUES (the_checksum, the_entity) RETURNING id INTO the_id;
          RETURN the_id;
        EXCEPTION WHEN UNIQUE_VIOLATION THEN
        -- do nothing, just loop back to the update
        END;
      END LOOP;
    END;
    $$;

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
