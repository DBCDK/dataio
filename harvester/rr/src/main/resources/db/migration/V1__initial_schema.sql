CREATE TABLE task (
    id                      SERIAL,
    timeOfCreation          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timeOfCompletion        TIMESTAMP,
    submitterNumber         BIGINT NOT NULL,
    tag                     TEXT,
    status                  TEXT,
    basedOnJob              INTEGER,
    numberOfRecords         INTEGER,
    recordIds               JSONB,
    PRIMARY KEY (id)
);
CREATE INDEX task_timeOfCreation_index ON task(timeOfCreation);
CREATE INDEX task_tag_index ON task(tag);
CREATE INDEX task_status_index ON task(status);
