INSERT INTO batch(name) VALUES ('15--42-0-9');
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-9', 'data42-0-9', '{"id": "42-0-9"}', false);
UPDATE entry SET status='OK', diagnostics='[{"level": "OK", "message": "ok42-0-9"}]' WHERE id=1;
