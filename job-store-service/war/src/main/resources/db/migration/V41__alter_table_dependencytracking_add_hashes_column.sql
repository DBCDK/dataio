DROP INDEX dependency_tracking_matchkeys;
ALTER TABLE dependencytracking DROP COLUMN blocking;

ALTER TABLE dependencytracking ADD COLUMN hashes INTEGER[];
CREATE INDEX dependencytracking_hashes_index ON dependencytracking USING GIN (hashes);

ALTER TABLE dependencytracking ADD COLUMN submitter INTEGER NOT NULL DEFAULT 0;
CREATE INDEX dependencytracking_sinkid_submitter_index ON dependencyTracking(sinkid, submitter);
