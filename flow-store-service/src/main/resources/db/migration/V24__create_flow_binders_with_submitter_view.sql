CREATE OR REPLACE VIEW flow_binders_with_submitter AS
SELECT
    flow_binders.name_idx,
    flow_binders_submitters.flow_binder_id,
    flow_binders_submitters.submitter_id
FROM
    flow_binders,
    flow_binders_submitters
WHERE
    flow_binders_submitters.flow_binder_id = flow_binders.id;
