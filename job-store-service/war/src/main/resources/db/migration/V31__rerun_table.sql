CREATE TYPE rerun_state AS ENUM ('IN_PROGRESS','WAITING');

CREATE TABLE rerun (
  id                  SERIAL PRIMARY KEY,
  state               rerun_state NOT NULL DEFAULT 'WAITING',
  timeOfCreation      TIMESTAMP DEFAULT now(),
  jobId               INTEGER NOT NULL REFERENCES job(id),
  harvesterId         INTEGER
);
CREATE INDEX rerun_state_index ON rerun(state);
