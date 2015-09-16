ALTER TABLE notification ALTER COLUMN destination DROP NOT NULL;
CREATE INDEX notification_jobId_index ON notification(jobId);
