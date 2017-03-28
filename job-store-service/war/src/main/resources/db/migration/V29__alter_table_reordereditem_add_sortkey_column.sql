ALTER TABLE reordereditem DROP CONSTRAINT reordereditem_pkey;
ALTER TABLE reordereditem DROP COLUMN seqno;
ALTER TABLE reordereditem ADD COLUMN id SERIAL PRIMARY KEY;
ALTER TABLE reordereditem ADD COLUMN sortkey INTEGER NOT NULL DEFAULT 0;
CREATE INDEX reordereditem_jobid_sortkey_index ON reordereditem(jobid, sortkey);
