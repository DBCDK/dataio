CREATE TABLE jobqueue (
  id                      SERIAL PRIMARY KEY,
  timeOfEntry             TIMESTAMP NOT NULL,
  sinkId                  INTEGER NOT NULL,
  jobId                   INTEGER UNIQUE NOT NULL REFERENCES job(id),
  state                   TEXT NOT NULL,
  sequenceAnalysis        BOOLEAN NOT NULL,
  recordSplitterType      TEXT NOT NULL
);