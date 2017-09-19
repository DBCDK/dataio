DROP INDEX task_status_index;
ALTER TABLE task DROP COLUMN status;

DROP INDEX task_tag_index;
ALTER TABLE task DROP COLUMN tag;

ALTER TABLE task DROP COLUMN timeOfCompletion;
