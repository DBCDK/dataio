-- An item that arrived without a record key has a null one here, which is what tells job-store
-- it has no watermark row rather than that its outcome must not advance one.

INSERT INTO batch(name) VALUES ('15--42-0-9');
INSERT INTO staged_item(batch,sink_id,record_key,job_id,chunk_id,item_id) VALUES (1, 15, NULL, 42, 0, 9);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-9', 'data42-0-9', '{"id": "42-0-9"}', false);
UPDATE entry SET status='OK', diagnostics='[{"level": "OK", "message": "ok42-0-9"}]' WHERE id=1;
