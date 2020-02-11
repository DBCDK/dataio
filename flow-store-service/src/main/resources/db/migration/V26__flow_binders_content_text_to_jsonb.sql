ALTER TABLE flow_binders ALTER COLUMN content TYPE JSONB USING content::JSONB;

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

alter table flow_binders drop COLUMN name_idx;
create unique index on flow_binders((content->>'name'));
