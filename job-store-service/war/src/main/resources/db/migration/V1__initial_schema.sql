CREATE TYPE flow_cacheline AS (
	id integer,
	checksum text,
	flow json
);

CREATE TYPE rerun_state AS ENUM (
    'IN_PROGRESS',
    'WAITING'
);

CREATE TYPE sink_cacheline AS (
	id integer,
	checksum text,
	sink json
);

CREATE FUNCTION dbc_jsonb_append(jsonb, jsonb) RETURNS jsonb
    LANGUAGE sql
    AS $_$
     WITH json_union AS
       (SELECT * FROM jsonb_each_text($1)
          UNION ALL
        SELECT * FROM jsonb_each_text($2))
     SELECT json_object_agg(key, value)::jsonb FROM json_union;
   $_$;

CREATE FUNCTION dbc_jsonb_append_key_value(jsonb, text, text) RETURNS jsonb
    LANGUAGE sql
    AS $_$
     SELECT dbc_jsonb_append($1, json_build_object($2, $3)::jsonb);
   $_$;

CREATE FUNCTION dbc_jsonb_append_key_value_pairs(jsonb, VARIADIC text[]) RETURNS jsonb
    LANGUAGE sql
    AS $_$
     SELECT dbc_jsonb_append($1, json_object($2)::jsonb);
   $_$;

CREATE FUNCTION set_flowcache(the_checksum text, the_flow json) RETURNS flow_cacheline
    LANGUAGE plpgsql
    AS $$
    DECLARE
      the_cacheline flow_cacheline;
    BEGIN
      LOOP
        UPDATE flowcache SET checksum=the_checksum WHERE checksum=the_checksum RETURNING id, checksum, flow INTO the_cacheline;
        IF FOUND THEN
          RETURN the_cacheline;
        END IF;
        -- not found, try inserting instead and check exception in case of race condition
        BEGIN
          INSERT INTO flowcache (checksum, flow) VALUES (the_checksum, the_flow) RETURNING id, checksum, flow INTO the_cacheline;
          RETURN the_cacheline;
        EXCEPTION WHEN UNIQUE_VIOLATION THEN
        -- do nothing, just loop back to the update
        END;
      END LOOP;
    END;
    $$;

CREATE FUNCTION set_sinkcache(the_checksum text, the_sink json) RETURNS sink_cacheline
    LANGUAGE plpgsql
    AS $$
    DECLARE
      the_cacheline sink_cacheline;
    BEGIN
      LOOP
        UPDATE sinkcache SET checksum=the_checksum WHERE checksum=the_checksum RETURNING id, checksum, sink INTO the_cacheline;
        IF FOUND THEN
          RETURN the_cacheline;
        END IF;
        -- not found, try inserting instead and check exception in case of race condition
        BEGIN
          INSERT INTO sinkcache (checksum, sink) VALUES (the_checksum, the_sink) RETURNING id, checksum, sink INTO the_cacheline;
          RETURN the_cacheline;
        EXCEPTION WHEN UNIQUE_VIOLATION THEN
        -- do nothing, just loop back to the update
        END;
      END LOOP;
    END;
    $$;

CREATE FUNCTION update_timeoflastmodification() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        NEW.timeOfLastModification = timeofday()::TIMESTAMP;
        RETURN NEW;
    END;
    $$;

CREATE TABLE chunk (
    id integer NOT NULL,
    jobid integer NOT NULL,
    datafileid text NOT NULL,
    numberofitems smallint DEFAULT 10 NOT NULL,
    timeofcreation timestamp without time zone DEFAULT now(),
    timeofcompletion timestamp without time zone,
    timeoflastmodification timestamp without time zone DEFAULT (timeofday())::timestamp without time zone,
    sequenceanalysisdata json NOT NULL,
    state json NOT NULL
);

CREATE TABLE dependencytracking (
    jobid integer NOT NULL,
    chunkid integer NOT NULL,
    sinkid integer NOT NULL,
    status smallint NOT NULL,
    waitingon jsonb,
    matchkeys jsonb,
    priority integer DEFAULT 4 NOT NULL,
    hashes integer[],
    submitter integer DEFAULT 0 NOT NULL
);

CREATE TABLE flowcache (
    id integer NOT NULL,
    checksum text NOT NULL,
    flow json NOT NULL
);

CREATE SEQUENCE flowcache_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE flowcache_id_seq OWNED BY flowcache.id;

CREATE TABLE item (
    id smallint NOT NULL,
    chunkid integer NOT NULL,
    jobid integer NOT NULL,
    timeofcreation timestamp without time zone DEFAULT now(),
    timeofcompletion timestamp without time zone,
    timeoflastmodification timestamp without time zone DEFAULT (timeofday())::timestamp without time zone,
    state json NOT NULL,
    partitioningoutcome jsonb,
    processingoutcome jsonb,
    deliveringoutcome jsonb,
    nextprocessingoutcome jsonb,
    workflownote jsonb,
    recordinfo jsonb,
    positionindatafile integer
);

CREATE TABLE job (
    id integer NOT NULL,
    eoj boolean DEFAULT true NOT NULL,
    partnumber integer DEFAULT 0 NOT NULL,
    numberofchunks integer DEFAULT 0 NOT NULL,
    numberofitems integer DEFAULT 0 NOT NULL,
    timeofcreation timestamp without time zone DEFAULT now(),
    timeofcompletion timestamp without time zone,
    timeoflastmodification timestamp without time zone DEFAULT (timeofday())::timestamp without time zone,
    specification jsonb NOT NULL,
    state json NOT NULL,
    cachedflow integer,
    cachedsink integer,
    flowstorereferences json NOT NULL,
    fatalerror boolean DEFAULT false NOT NULL,
    workflownote jsonb,
    priority integer DEFAULT 4 NOT NULL,
    skipped integer
);

CREATE SEQUENCE job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE job_id_seq OWNED BY job.id;

CREATE TABLE jobqueue (
    id integer NOT NULL,
    timeofentry timestamp without time zone NOT NULL,
    sinkid integer NOT NULL,
    jobid integer NOT NULL,
    state text NOT NULL,
    recordsplittertype text NOT NULL,
    retries integer DEFAULT 0,
    includefilter bytea
);

CREATE SEQUENCE jobqueue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE jobqueue_id_seq OWNED BY jobqueue.id;

CREATE TABLE notification (
    id integer NOT NULL,
    timeofcreation timestamp without time zone DEFAULT now(),
    timeoflastmodification timestamp without time zone DEFAULT (timeofday())::timestamp without time zone,
    type smallint NOT NULL,
    status smallint NOT NULL,
    statusmessage text,
    destination text,
    content text,
    job integer,
    jobid integer,
    context text
);

CREATE SEQUENCE notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE notification_id_seq OWNED BY notification.id;

CREATE TABLE reordereditem (
    jobid integer NOT NULL,
    chunkitem jsonb NOT NULL,
    recordinfo jsonb NOT NULL,
    id integer NOT NULL,
    sortkey integer DEFAULT 0 NOT NULL,
    positionindatafile integer
);

CREATE SEQUENCE reordereditem_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE reordereditem_id_seq OWNED BY reordereditem.id;

CREATE TABLE rerun (
    id integer NOT NULL,
    state rerun_state DEFAULT 'WAITING'::rerun_state NOT NULL,
    timeofcreation timestamp without time zone DEFAULT now(),
    jobid integer NOT NULL,
    includefailedonly boolean
);

CREATE SEQUENCE rerun_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE rerun_id_seq OWNED BY rerun.id;

CREATE TABLE sinkcache (
    id integer NOT NULL,
    checksum text NOT NULL,
    sink json NOT NULL
);

CREATE SEQUENCE sinkcache_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE sinkcache_id_seq OWNED BY sinkcache.id;

ALTER TABLE ONLY flowcache ALTER COLUMN id SET DEFAULT nextval('flowcache_id_seq'::regclass);

ALTER TABLE ONLY job ALTER COLUMN id SET DEFAULT nextval('job_id_seq'::regclass);

ALTER TABLE ONLY jobqueue ALTER COLUMN id SET DEFAULT nextval('jobqueue_id_seq'::regclass);

ALTER TABLE ONLY notification ALTER COLUMN id SET DEFAULT nextval('notification_id_seq'::regclass);

ALTER TABLE ONLY reordereditem ALTER COLUMN id SET DEFAULT nextval('reordereditem_id_seq'::regclass);

ALTER TABLE ONLY rerun ALTER COLUMN id SET DEFAULT nextval('rerun_id_seq'::regclass);

ALTER TABLE ONLY sinkcache ALTER COLUMN id SET DEFAULT nextval('sinkcache_id_seq'::regclass);

ALTER TABLE ONLY chunk
    ADD CONSTRAINT chunk_pkey PRIMARY KEY (jobid, id);

ALTER TABLE ONLY dependencytracking
    ADD CONSTRAINT dependencytracking_pkey PRIMARY KEY (jobid, chunkid);

ALTER TABLE ONLY flowcache
    ADD CONSTRAINT flowcache_checksum_key UNIQUE (checksum);

ALTER TABLE ONLY flowcache
    ADD CONSTRAINT flowcache_pkey PRIMARY KEY (id);

ALTER TABLE ONLY item
    ADD CONSTRAINT item_pkey PRIMARY KEY (jobid, chunkid, id);

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pkey PRIMARY KEY (id);

ALTER TABLE ONLY jobqueue
    ADD CONSTRAINT jobqueue_jobid_key UNIQUE (jobid);

ALTER TABLE ONLY jobqueue
    ADD CONSTRAINT jobqueue_pkey PRIMARY KEY (id);

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);

ALTER TABLE ONLY reordereditem
    ADD CONSTRAINT reordereditem_pkey PRIMARY KEY (id);

ALTER TABLE ONLY rerun
    ADD CONSTRAINT rerun_pkey PRIMARY KEY (id);

ALTER TABLE ONLY sinkcache
    ADD CONSTRAINT sinkcache_checksum_key UNIQUE (checksum);

ALTER TABLE ONLY sinkcache
    ADD CONSTRAINT sinkcache_pkey PRIMARY KEY (id);

CREATE INDEX chunk_timeofcreationforunfinishedchunks_index ON chunk USING btree (timeofcreation) WHERE (timeofcompletion IS NULL);

CREATE INDEX chunk_timeoflastmodification_index ON chunk USING btree (timeoflastmodification);

CREATE INDEX dependencytracking_hashes_index ON dependencytracking USING gin (hashes);

CREATE INDEX dependencytracking_sinkid_status_index ON dependencytracking USING btree (sinkid, status);

CREATE INDEX dependencytracking_sinkid_submitter_index ON dependencytracking USING btree (sinkid, submitter);

CREATE INDEX item_id_index ON item USING btree (((recordinfo ->> 'id'::text)));

CREATE INDEX item_statefailed_index ON item USING btree (jobid, chunkid, id) WHERE (((((state -> 'states'::text) -> 'PARTITIONING'::text) ->> 'failed'::text) <> '0'::text) OR ((((state -> 'states'::text) -> 'PROCESSING'::text) ->> 'failed'::text) <> '0'::text) OR ((((state -> 'states'::text) -> 'DELIVERING'::text) ->> 'failed'::text) <> '0'::text));

CREATE INDEX item_stateignored_index ON item USING btree (jobid, chunkid, id) WHERE (((((state -> 'states'::text) -> 'PARTITIONING'::text) ->> 'ignored'::text) <> '0'::text) OR ((((state -> 'states'::text) -> 'PROCESSING'::text) ->> 'ignored'::text) <> '0'::text) OR ((((state -> 'states'::text) -> 'DELIVERING'::text) ->> 'ignored'::text) <> '0'::text));

CREATE INDEX job_creation_failed_index ON job USING btree (id) WHERE ((fatalerror = true) OR ((((state -> 'states'::text) -> 'PARTITIONING'::text) ->> 'failed'::text) <> '0'::text));

CREATE INDEX job_deliveringfailed_index ON job USING btree (id) WHERE ((((state -> 'states'::text) -> 'DELIVERING'::text) ->> 'failed'::text) <> '0'::text);

CREATE INDEX job_processingfailed_index ON job USING btree (id) WHERE ((((state -> 'states'::text) -> 'PROCESSING'::text) ->> 'failed'::text) <> '0'::text);

CREATE INDEX job_specification_idx ON job USING gin (specification jsonb_path_ops);

CREATE INDEX job_timeofcreation_index ON job USING btree (timeofcreation);

CREATE INDEX job_timeoflastmodification_index ON job USING btree (timeoflastmodification);

CREATE INDEX job_unfinished_index ON job USING btree (id) WHERE (timeofcompletion IS NULL);

CREATE INDEX notification_jobid_index ON notification USING btree (jobid);

CREATE INDEX notification_status_index ON notification USING btree (status);

CREATE INDEX notification_type_index ON notification USING btree (type);

CREATE INDEX preview_only_index ON job USING btree (id) WHERE ((numberofchunks = 0) AND (numberofitems <> 0));

CREATE INDEX reordereditem_jobid_sortkey_index ON reordereditem USING btree (jobid, sortkey);

CREATE INDEX reordereditem_recordinfo_idx ON reordereditem USING gin (recordinfo jsonb_path_ops);

CREATE INDEX rerun_state_index ON rerun USING btree (state);

CREATE INDEX sink_reference_index ON job USING btree ((((((flowstorereferences -> 'references'::text) -> 'SINK'::text) ->> 'id'::text))::integer));

CREATE TRIGGER item_timeoflastmodification_trigger BEFORE UPDATE ON item FOR EACH ROW EXECUTE PROCEDURE update_timeoflastmodification();

CREATE TRIGGER job_timeoflastmodification_trigger BEFORE UPDATE ON job FOR EACH ROW EXECUTE PROCEDURE update_timeoflastmodification();

ALTER TABLE ONLY dependencytracking
    ADD CONSTRAINT dependencytracking_jobid_fkey FOREIGN KEY (jobid, chunkid) REFERENCES chunk(jobid, id) ON DELETE CASCADE;

ALTER TABLE ONLY job
    ADD CONSTRAINT job_cachedflow_fkey FOREIGN KEY (cachedflow) REFERENCES flowcache(id);

ALTER TABLE ONLY job
    ADD CONSTRAINT job_cachedsink_fkey FOREIGN KEY (cachedsink) REFERENCES sinkcache(id);

ALTER TABLE ONLY item
    ADD CONSTRAINT jobid_chunk_fk FOREIGN KEY (chunkid, jobid) REFERENCES chunk(id, jobid) ON DELETE CASCADE;

ALTER TABLE ONLY chunk
    ADD CONSTRAINT jobid_fk FOREIGN KEY (jobid) REFERENCES job(id) ON DELETE CASCADE;

ALTER TABLE ONLY jobqueue
    ADD CONSTRAINT jobqueue_jobid_fkey FOREIGN KEY (jobid) REFERENCES job(id);

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_job_fkey FOREIGN KEY (job) REFERENCES job(id) ON DELETE CASCADE;

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_jobid_fkey FOREIGN KEY (jobid) REFERENCES job(id);

ALTER TABLE ONLY rerun
    ADD CONSTRAINT rerun_jobid_fkey FOREIGN KEY (jobid) REFERENCES job(id);
