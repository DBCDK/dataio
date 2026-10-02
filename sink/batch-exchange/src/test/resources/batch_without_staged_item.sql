-- A batch this sink has no bookkeeping row for. Its item cannot be addressed, so there is
-- nothing to report it against.

INSERT INTO batch(name,status) VALUES ('a batch no item is recorded as staged for', 'COMPLETED');
