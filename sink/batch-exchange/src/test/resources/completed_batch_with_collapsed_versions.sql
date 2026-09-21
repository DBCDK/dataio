-- A completed batch whose item reached the batch exchange only after two other versions of the
-- same record were skipped to arrive at it. What was skipped travels with the staged item so
-- that its outcome can say so.

INSERT INTO batch(name) VALUES ('15-870970:1-44-0-1');
INSERT INTO staged_item(batch,sink_id,record_key,job_id,chunk_id,item_id,collapsed_count,collapsed_from)
VALUES (1, 15, '870970:1', 44, 0, 1, 2, '42/0/1');
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '44-0-1', 'data44-0-1', '{"id": "44-0-1"}', false);
UPDATE entry SET status='FAILED', diagnostics='[{"level": "ERROR", "message": "error44-0-1"}]' WHERE id=1;
