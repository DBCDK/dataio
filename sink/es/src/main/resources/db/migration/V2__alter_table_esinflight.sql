ALTER TABLE esinflight ADD COLUMN sinkid bigint NOT NULL, ADD COLUMN databasename text NOT NULL;
ALTER TABLE esinflight DROP CONSTRAINT esinflight_pkey;
ALTER TABLE esinflight ADD CONSTRAINT esinflight_pkey PRIMARY KEY (targetreference, sinkid);
ALTER TABLE esinflight DROP COLUMN resourcename, DROP COLUMN recordslots;
ALTER TABLE esinflight RENAME COLUMN sinkchunkresult TO chunk;