-- A completed batch holding version 42/0/1 of a record. The test adds the version held behind
-- it, since the held records are addi and are better built than spelled out here.

INSERT INTO batch(name) VALUES ('15-870970:1-42-0-1');
INSERT INTO staged_item(batch,sink_id,record_key,job_id,chunk_id,item_id) VALUES (1, 15, '870970:1', 42, 0, 1);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-1', 'data42-0-1', '{"id": "42-0-1"}', false);
UPDATE entry SET status='OK', diagnostics='[{"level": "OK", "message": "ok42-0-1"}]' WHERE id=1;
