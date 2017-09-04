INSERT INTO task (configId, status, records) VALUES (42, 'WAITING', '[]');
INSERT INTO task (configId, status, basedOnJob, records) VALUES (42, 'READY', 999999, '[{"submitter": 123456, "bibliographicRecordId": "test1"}]');
INSERT INTO task (configId, status, records) VALUES (42, 'READY', '[]');