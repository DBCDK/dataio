DROP INDEX job_fatalerror_index;
CREATE INDEX job_fatalError_index ON job(id) WHERE fatalError = 't';