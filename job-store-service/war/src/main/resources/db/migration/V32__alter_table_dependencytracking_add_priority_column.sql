ALTER TABLE dependencyTracking ADD COLUMN priority INTEGER NOT NULL DEFAULT 4;
CREATE INDEX dependencyTracking_sinkId_status_index ON dependencyTracking(sinkId, status);
