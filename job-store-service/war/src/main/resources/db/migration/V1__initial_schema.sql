-- 
-- DataIO - Data IO
-- Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
-- Denmark. CVR: 15149043
--
-- This file is part of DataIO.
--
-- DataIO is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- DataIO is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
-- 
CREATE TABLE flowcache (
    id                      SERIAL,
    checksum                TEXT UNIQUE NOT NULL,
    flow                    JSON NOT NULL,
    PRIMARY KEY (id)
);
DROP TYPE IF EXISTS flow_cacheline CASCADE;
CREATE TYPE flow_cacheline AS (id INTEGER, checksum TEXT, flow JSON);

CREATE TABLE sinkcache (
    id                      SERIAL,
    checksum                TEXT UNIQUE NOT NULL,
    sink                    JSON NOT NULL,
    PRIMARY KEY (id)
);
DROP TYPE IF EXISTS sink_cacheline CASCADE;
CREATE TYPE sink_cacheline AS (id INTEGER, checksum TEXT, sink JSON);

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
    cachedFlow              INTEGER REFERENCES flowcache(id),
    cachedSink              INTEGER REFERENCES sinkcache(id),
    flowStoreReferences     JSON NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX job_timeOfCreation_index ON job(timeOfCreation);
CREATE INDEX job_timeOfLastModification_index ON job(timeOfLastModification);

CREATE TABLE chunk (
    id                      INTEGER NOT NULL,
    jobId                   INTEGER NOT NULL,
    dataFileId              TEXT NOT NULL,
    numberOfItems           SMALLINT NOT NULL DEFAULT 10,
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
       state->'states'->'PARTITIONING'->>'failed' != '0'
    OR state->'states'->'PROCESSING'->>'failed' != '0'
    OR state->'states'->'DELIVERING'->>'failed' != '0';

CREATE INDEX item_stateIgnored_index ON item(jobId, chunkId, id) WHERE
       state->'states'->'PARTITIONING'->>'ignored' != '0'
    OR state->'states'->'PROCESSING'->>'ignored' != '0'
    OR state->'states'->'DELIVERING'->>'ignored' != '0';

CREATE INDEX job_processingFailed_index ON job(id) WHERE
       state->'states'->'PROCESSING'->>'failed' != '0';

CREATE INDEX job_deliveringFailed_index ON job(id) WHERE
       state->'states'->'DELIVERING'->>'failed' != '0';

CREATE OR REPLACE FUNCTION set_flowcache(the_checksum TEXT, the_flow JSON)
    RETURNS flow_cacheline AS
    $BODY$
    DECLARE
      the_cacheline flow_cacheline;
    BEGIN
      LOOP
        UPDATE flowcache SET checksum=the_checksum WHERE checksum=the_checksum RETURNING id, checksum, flow INTO the_cacheline;
        IF FOUND THEN
          RETURN the_cacheline;
        END IF;
        -- not found, try inserting instead and check exception in case of race condition
        BEGIN
          INSERT INTO flowcache (checksum, flow) VALUES (the_checksum, the_flow) RETURNING id, checksum, flow INTO the_cacheline;
          RETURN the_cacheline;
        EXCEPTION WHEN UNIQUE_VIOLATION THEN
        -- do nothing, just loop back to the update
        END;
      END LOOP;
    END;
    $BODY$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION set_sinkcache(the_checksum TEXT, the_sink JSON)
    RETURNS sink_cacheline AS
    $BODY$
    DECLARE
      the_cacheline sink_cacheline;
    BEGIN
      LOOP
        UPDATE sinkcache SET checksum=the_checksum WHERE checksum=the_checksum RETURNING id, checksum, sink INTO the_cacheline;
        IF FOUND THEN
          RETURN the_cacheline;
        END IF;
        -- not found, try inserting instead and check exception in case of race condition
        BEGIN
          INSERT INTO sinkcache (checksum, sink) VALUES (the_checksum, the_sink) RETURNING id, checksum, sink INTO the_cacheline;
          RETURN the_cacheline;
        EXCEPTION WHEN UNIQUE_VIOLATION THEN
        -- do nothing, just loop back to the update
        END;
      END LOOP;
    END;
    $BODY$
    LANGUAGE plpgsql;

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
