CREATE INDEX job_unfinished_index ON job(id) WHERE timeOfCompletion IS NULL;