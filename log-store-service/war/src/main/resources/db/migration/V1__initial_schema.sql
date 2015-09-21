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
CREATE TABLE LOGENTRY (
    ID                BIGSERIAL PRIMARY KEY,
    TIMESTAMP         TIMESTAMP NOT NULL,
    FORMATTED_MESSAGE TEXT,
    THREAD_NAME       TEXT,
    LOGGER_NAME       TEXT,
    LEVEL_STRING      TEXT,
    CALLER_FILENAME   TEXT,
    CALLER_CLASS      TEXT,
    CALLER_METHOD     TEXT,
    CALLER_LINE       TEXT,
    STACK_TRACE       TEXT,
    MDC               TEXT,
    JOB_ID            TEXT,
    CHUNK_ID          BIGINT,
    ITEM_ID           BIGINT
);

CREATE INDEX TIMESTAMP_IDX ON LOGENTRY(TIMESTAMP);
CREATE INDEX TRACKING_ID_IDX ON LOGENTRY(JOB_ID, CHUNK_ID, ITEM_ID);


