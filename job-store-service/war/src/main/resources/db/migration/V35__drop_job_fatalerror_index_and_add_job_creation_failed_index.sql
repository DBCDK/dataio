DROP INDEX job_fatalerror_index;
DROP INDEX job_partitioningFailed_index;
CREATE INDEX job_creation_failed_index ON job(id) WHERE fatalError = 't' OR state->'states'->'PARTITIONING'->>'failed' != '0';