ALTER TABLE flow_binders ALTER COLUMN content TYPE JSONB USING content::JSONB;
CREATE INDEX flow_binders_content_index ON flow_binders USING GIN (content jsonb_path_ops);

CREATE OR REPLACE VIEW flow_binders_with_submitter AS
SELECT
    flow_binders.content->>'name' as name_idx,
    flow_binders_submitters.flow_binder_id,
    flow_binders_submitters.submitter_id
FROM
    flow_binders,
    flow_binders_submitters
WHERE
    flow_binders_submitters.flow_binder_id = flow_binders.id;

ALTER TABLE flow_binders DROP COLUMN name_idx;
CREATE UNIQUE INDEX ON flow_binders((content->>'name'));

DROP TABLE flow_binders_search_index;
