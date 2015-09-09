CREATE TABLE notification (
    id                      SERIAL,
    timeOfCreation          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    timeOfLastModification  TIMESTAMP DEFAULT timeofday()::TIMESTAMP,
    type                    SMALLINT NOT NULL,
    status                  SMALLINT NOT NULL,
    statusMessage           TEXT,
    destination             TEXT NOT NULL,
    content                 TEXT,
    job                     INTEGER REFERENCES job(id) ON DELETE CASCADE,
    jobId                   INTEGER REFERENCES job(id),
    PRIMARY KEY (id)
);
CREATE INDEX notification_status_index ON notification(status);
