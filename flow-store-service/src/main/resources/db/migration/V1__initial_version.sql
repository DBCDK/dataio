CREATE FUNCTION check_no_flow_binder_references_submitter() RETURNS trigger
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

CREATE FUNCTION check_no_flow_references_component() RETURNS trigger
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

CREATE TABLE flow_binders (
    id bigint NOT NULL,
    content jsonb NOT NULL,
    version bigint NOT NULL,
    flow_id bigint,
    sink_id bigint
);

CREATE TABLE flow_components (
    id bigint NOT NULL,
    content jsonb NOT NULL,
    version bigint NOT NULL,
    next jsonb,
    view jsonb
);

CREATE TABLE flows (
    id bigint NOT NULL,
    content jsonb NOT NULL,
    version bigint NOT NULL,
    view jsonb
);

CREATE TABLE gatekeeper_destinations (
    id bigint NOT NULL,
    submitternumber text NOT NULL,
    destination text NOT NULL,
    packaging text NOT NULL,
    format text NOT NULL,
    copytoposthus boolean DEFAULT false NOT NULL,
    notifyfromposthus boolean DEFAULT false NOT NULL
);

CREATE TABLE harvester_configs (
    id bigint NOT NULL,
    version bigint NOT NULL,
    type text,
    content jsonb
);

CREATE TABLE sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0)
);

CREATE TABLE sinks (
    id bigint NOT NULL,
    content jsonb NOT NULL,
    version bigint NOT NULL
);

CREATE TABLE submitters (
    id bigint NOT NULL,
    content jsonb NOT NULL,
    version bigint NOT NULL
);

ALTER TABLE ONLY flow_binders
    ADD CONSTRAINT flow_binders_pkey PRIMARY KEY (id);

ALTER TABLE ONLY flow_components
    ADD CONSTRAINT flow_components_pkey PRIMARY KEY (id);

ALTER TABLE ONLY flows
    ADD CONSTRAINT flows_pkey PRIMARY KEY (id);

ALTER TABLE ONLY gatekeeper_destinations
    ADD CONSTRAINT gatekeeper_destinations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY harvester_configs
    ADD CONSTRAINT harvester_config_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sequence
    ADD CONSTRAINT sequence_pkey PRIMARY KEY (seq_name);

ALTER TABLE ONLY sinks
    ADD CONSTRAINT sinks_pkey PRIMARY KEY (id);

ALTER TABLE ONLY submitters
    ADD CONSTRAINT submitters_pkey PRIMARY KEY (id);

ALTER TABLE ONLY gatekeeper_destinations
    ADD CONSTRAINT unique_constraint_gatekeeper_destinations UNIQUE (submitternumber, destination, packaging, format);

CREATE INDEX flow_binders_content_index ON flow_binders USING gin (content jsonb_path_ops);

CREATE UNIQUE INDEX flow_binders_expr_idx ON flow_binders USING btree (((content ->> 'name'::text)));

CREATE UNIQUE INDEX flow_components_expr_idx ON flow_components USING btree (((content ->> 'name'::text)));

CREATE INDEX flow_components_view_index ON flow_components USING gin (view jsonb_path_ops);

CREATE UNIQUE INDEX flows_expr_idx ON flows USING btree (((content ->> 'name'::text)));

CREATE INDEX flows_view_index ON flows USING gin (view jsonb_path_ops);

CREATE INDEX harvester_config_content ON harvester_configs USING gin (content jsonb_path_ops);

CREATE UNIQUE INDEX name_index ON sinks USING btree (((content ->> 'name'::text)));

CREATE INDEX submitters_content_index ON submitters USING gin (content jsonb_path_ops);

CREATE UNIQUE INDEX submitters_expr_idx ON submitters USING btree (((content ->> 'name'::text)));

CREATE UNIQUE INDEX submitters_expr_idx1 ON submitters USING btree (((content ->> 'number'::text)));

CREATE CONSTRAINT TRIGGER flow_components_delete_constraint_trigger AFTER DELETE ON flow_components NOT DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW EXECUTE PROCEDURE check_no_flow_references_component();

CREATE CONSTRAINT TRIGGER submitters_delete_constraint_trigger AFTER DELETE ON submitters NOT DEFERRABLE INITIALLY IMMEDIATE FOR EACH ROW EXECUTE PROCEDURE check_no_flow_binder_references_submitter();

ALTER TABLE ONLY flow_binders
    ADD CONSTRAINT fk_flow_binders_flow_id FOREIGN KEY (flow_id) REFERENCES flows(id);

ALTER TABLE ONLY flow_binders
    ADD CONSTRAINT fk_flow_binders_sink_id FOREIGN KEY (sink_id) REFERENCES sinks(id);

INSERT INTO sequence (seq_name,seq_count) VALUES ('SEQ_GEN',1200);
