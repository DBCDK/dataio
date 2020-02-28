ALTER TABLE submitters ALTER COLUMN content TYPE JSONB USING content::JSONB;
CREATE INDEX submitters_content_index ON submitters USING GIN (content jsonb_path_ops);
ALTER TABLE submitters DROP COLUMN name_idx;
ALTER TABLE submitters DROP COLUMN number_idx;
CREATE UNIQUE INDEX ON submitters((content->>'name'));