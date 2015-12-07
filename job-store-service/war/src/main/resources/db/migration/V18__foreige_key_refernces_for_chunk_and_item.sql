
ALTER TABLE chunk DROP CONSTRAINT IF EXISTS jobid_fk;
ALTER TABLE item DROP CONSTRAINT IF EXISTS jobid_chunk_fk;

ALTER TABLE chunk ADD CONSTRAINT jobid_fk FOREIGN KEY (jobid) REFERENCES job (id) ON DELETE cascade;
ALTER TABLE item ADD CONSTRAINT jobid_chunk_fk FOREIGN KEY (chunkid,jobid) REFERENCES chunk(id,jobid) on delete cascade;

