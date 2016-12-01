INSERT INTO batch(name) VALUES ('42-0');

INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-1', 'data42-0-1', '{"id": "42-0-1"}', false);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-2', 'data42-0-2', '{"id": "42-0-2"}', false);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-3', 'data42-0-3', '{"id": "42-0-3"}', false);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-4', 'data42-0-4', '{"id": "42-0-4"}', false);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-5', 'data42-0-5a', '{"id": "42-0-5a"}', true);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-5', 'data42-0-5b', '{"id": "42-0-5b"}', true);
INSERT INTO entry(batch,trackingId,content,metadata,isContinued) VALUES (1, '42-0-5', 'data42-0-5c', '{"id": "42-0-5c"}', false);

UPDATE entry SET status='IGNORED', diagnostics='[{"level": "OK", "message": "ok42-0-1"}]' WHERE id = 1;
UPDATE entry SET status='OK',      diagnostics='[{"level": "OK", "message": "ok42-0-2"}, {"level": "WARNING", "message": "warning42-0-2"}]' WHERE id=2;
UPDATE entry SET status='OK',      diagnostics='[{"level": "ERROR", "message": "error42-0-3"}]' WHERE id=3;
UPDATE entry SET status='FAILED',  diagnostics='[{"level": "ERROR", "message": "error42-0-4"}]' WHERE id=4;
UPDATE entry SET status='FAILED',  diagnostics='[{"level": "ERROR", "message": "error42-0-5a"}]' WHERE id=5;
UPDATE entry SET status='FAILED',  diagnostics='[{"level": "ERROR", "message": "error42-0-5b"}]' WHERE id=6;
UPDATE entry SET status='OK',      diagnostics='[{"level": "OK", "message": "ok42-0-5c"}]' WHERE id=7;
