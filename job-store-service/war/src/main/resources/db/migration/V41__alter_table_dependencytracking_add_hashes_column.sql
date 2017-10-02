DROP INDEX dependency_tracking_matchkeys;
ALTER TABLE dependencytracking DROP COLUMN blocking;

CREATE EXTENSION IF NOT EXISTS intarray;
ALTER TABLE dependencytracking ADD COLUMN hashes INT[];
CREATE INDEX dependencytracking_hashes_index ON dependencytracking USING GIN (hashes gin__int_ops);