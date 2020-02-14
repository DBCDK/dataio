ALTER TABLE flow_binders ALTER COLUMN content TYPE JSONB USING content::JSONB;
CREATE INDEX flow_binders_content_index ON flow_binders USING GIN (content jsonb_path_ops);

DROP VIEW flow_binders_with_submitter;

ALTER TABLE flow_binders DROP COLUMN name_idx;
CREATE UNIQUE INDEX ON flow_binders((content->>'name'));

DROP TABLE flow_binders_search_index;

ALTER TABLE flow_binders_submitters DROP CONSTRAINT IF EXISTS fk_flow_binders_submitters_flow_binder_id;
ALTER TABLE flow_binders_submitters DROP CONSTRAINT IF EXISTS fk_flow_binders_submitters_submitter_id;
DROP TABLE flow_binders_submitters;

CREATE OR REPLACE FUNCTION check_no_flow_binder_references_submitter() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    DECLARE
        criteria TEXT := '{"submitterIds": [' || OLD.id || ']}';
    BEGIN
        IF EXISTS(
            SELECT id FROM flow_binders
                WHERE content @> criteria::jsonb)
        THEN
            RAISE EXCEPTION 'submitter is still referenced by flow binders: %', OLD.id USING ERRCODE='23503';
        END IF;
        RETURN OLD;
    END
    $$;

CREATE CONSTRAINT TRIGGER submitters_delete_constraint_trigger
    AFTER DELETE ON submitters
    FOR EACH ROW
    EXECUTE PROCEDURE check_no_flow_binder_references_submitter();
