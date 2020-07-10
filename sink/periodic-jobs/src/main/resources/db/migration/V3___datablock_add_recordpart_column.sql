ALTER TABLE datablock ADD COLUMN recordPart INTEGER DEFAULT 0;
ALTER TABLE datablock DROP CONSTRAINT datablock_pkey;
ALTER TABLE datablock ADD PRIMARY KEY (jobId, recordNumber, recordPart);