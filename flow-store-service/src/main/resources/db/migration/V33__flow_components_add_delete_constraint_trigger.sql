CREATE OR REPLACE FUNCTION check_no_flow_references_component() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    DECLARE
        criteria TEXT := '{"components":[{"id":' || OLD.id || '}]}';
    BEGIN
        IF EXISTS(
            SELECT id FROM flows
                WHERE view @> criteria::jsonb)
        THEN
            RAISE EXCEPTION 'flow component is still referenced by flows: %', OLD.id USING ERRCODE='23503';
        END IF;
        RETURN OLD;
    END
    $$;

CREATE CONSTRAINT TRIGGER flow_components_delete_constraint_trigger
    AFTER DELETE ON flow_components
    FOR EACH ROW
    EXECUTE PROCEDURE check_no_flow_references_component();