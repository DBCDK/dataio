ALTER TABLE flow_binders ALTER COLUMN content TYPE JSONB USING content::JSONB;
CREATE INDEX flow_binders_content_index ON flow_binders USING GIN (content jsonb_path_ops);

DROP VIEW flow_binders_with_submitter;

ALTER TABLE flow_binders DROP COLUMN name_idx;
CREATE UNIQUE INDEX ON flow_binders((content->>'name'));

DROP TABLE flow_binders_search_index;
