--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: flow_cacheline; Type: TYPE; Schema: public; Owner: jobstore
--

CREATE TYPE flow_cacheline AS (
	id integer,
	checksum text,
	flow json
);

--
-- Name: sink_cacheline; Type: TYPE; Schema: public; Owner: jobstore
--

CREATE TYPE sink_cacheline AS (
	id integer,
	checksum text,
	sink json
);

--
-- Name: dbc_jsonb_append(jsonb, jsonb); Type: FUNCTION; Schema: public; Owner: jobstore
--

CREATE FUNCTION dbc_jsonb_append(jsonb, jsonb) RETURNS jsonb
    LANGUAGE sql
    AS $_$
     WITH json_union AS
       (SELECT * FROM jsonb_each_text($1)
          UNION ALL
        SELECT * FROM jsonb_each_text($2))
     SELECT json_object_agg(key, value)::jsonb FROM json_union;
   $_$;


--
-- Name: dbc_jsonb_append_key_value(jsonb, text, text); Type: FUNCTION; Schema: public; Owner: jobstore
--

CREATE FUNCTION dbc_jsonb_append_key_value(jsonb, text, text) RETURNS jsonb
    LANGUAGE sql
    AS $_$
     SELECT dbc_jsonb_append($1, json_build_object($2, $3)::jsonb);
   $_$;


--
-- Name: dbc_jsonb_append_key_value_pairs(jsonb, text[]); Type: FUNCTION; Schema: public; Owner: jobstore
--

CREATE FUNCTION dbc_jsonb_append_key_value_pairs(jsonb, VARIADIC text[]) RETURNS jsonb
    LANGUAGE sql
    AS $_$
     SELECT dbc_jsonb_append($1, json_object($2)::jsonb);
   $_$;

--
-- Name: set_flowcache(text, json); Type: FUNCTION; Schema: public; Owner: jobstore
--

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


--
-- Name: set_sinkcache(text, json); Type: FUNCTION; Schema: public; Owner: jobstore
--

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


--
-- Name: update_timeoflastmodification(); Type: FUNCTION; Schema: public; Owner: jobstore
--

CREATE FUNCTION update_timeoflastmodification() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
    BEGIN
        NEW.timeOfLastModification = timeofday()::TIMESTAMP;
        RETURN NEW;
    END;
    $$;


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: chunk; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

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



--
-- Name: company; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE TABLE company (
    id integer NOT NULL,
    name text NOT NULL,
    age integer NOT NULL,
    address character(50),
    salary real
);


--
-- Name: flowcache; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

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



--
-- Name: item; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE TABLE item (
    id smallint NOT NULL,
    chunkid integer NOT NULL,
    jobid integer NOT NULL,
    timeofcreation timestamp without time zone DEFAULT now(),
    timeofcompletion timestamp without time zone,
    timeoflastmodification timestamp without time zone DEFAULT (timeofday())::timestamp without time zone,
    state json NOT NULL,
    partitioningoutcome json,
    processingoutcome json,
    deliveringoutcome json,
    nextprocessingoutcome json
);


--
-- Name: job; Type: TABLE; Schema: public; Owner: jobstore; Tablespace:
--

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
    fatalerror boolean DEFAULT false NOT NULL
);



--
-- Name: job_id_seq; Type: SEQUENCE; Schema: public; Owner: jobstore
--

CREATE SEQUENCE job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: job_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: jobstore
--


--
-- Name: jobqueue; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE TABLE jobqueue (
    id integer NOT NULL,
    timeofentry timestamp without time zone NOT NULL,
    sinkid integer NOT NULL,
    jobid integer NOT NULL,
    state text NOT NULL,
    sequenceanalysis boolean NOT NULL,
    recordsplittertype text NOT NULL
);



--
-- Name: jobqueue_id_seq; Type: SEQUENCE; Schema: public; Owner: jobstore
--

CREATE SEQUENCE jobqueue_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: notification; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

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


--
-- Name: notification_id_seq; Type: SEQUENCE; Schema: public; Owner: jobstore
--

CREATE SEQUENCE notification_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: notification_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: jobstore
--

ALTER SEQUENCE notification_id_seq OWNED BY notification.id;


--
-- Name: schema_version; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE TABLE schema_version (
    installed_rank integer NOT NULL,
    version character varying(50) NOT NULL,
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: sinkcache; Type: TABLE; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE TABLE sinkcache (
    id integer NOT NULL,
    checksum text NOT NULL,
    sink json NOT NULL
);


--
-- Name: sinkcache_id_seq; Type: SEQUENCE; Schema: public; Owner: jobstore
--

CREATE SEQUENCE sinkcache_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



--
-- Name: id; Type: DEFAULT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY flowcache ALTER COLUMN id SET DEFAULT nextval('flowcache_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY job ALTER COLUMN id SET DEFAULT nextval('job_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY jobqueue ALTER COLUMN id SET DEFAULT nextval('jobqueue_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY notification ALTER COLUMN id SET DEFAULT nextval('notification_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY sinkcache ALTER COLUMN id SET DEFAULT nextval('sinkcache_id_seq'::regclass);


--
-- Name: chunk_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY chunk
    ADD CONSTRAINT chunk_pkey PRIMARY KEY (jobid, id);


--
-- Name: company_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY company
    ADD CONSTRAINT company_pkey PRIMARY KEY (id);


--
-- Name: flowcache_checksum_key; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY flowcache
    ADD CONSTRAINT flowcache_checksum_key UNIQUE (checksum);


--
-- Name: flowcache_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY flowcache
    ADD CONSTRAINT flowcache_pkey PRIMARY KEY (id);


--
-- Name: item_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY item
    ADD CONSTRAINT item_pkey PRIMARY KEY (jobid, chunkid, id);


--
-- Name: job_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY job
    ADD CONSTRAINT job_pkey PRIMARY KEY (id);


--
-- Name: jobqueue_jobid_key; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY jobqueue
    ADD CONSTRAINT jobqueue_jobid_key UNIQUE (jobid);


--
-- Name: jobqueue_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY jobqueue
    ADD CONSTRAINT jobqueue_pkey PRIMARY KEY (id);


--
-- Name: notification_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_pkey PRIMARY KEY (id);


--
-- Name: schema_version_pk; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY schema_version
    ADD CONSTRAINT schema_version_pk PRIMARY KEY (version);


--
-- Name: sinkcache_checksum_key; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY sinkcache
    ADD CONSTRAINT sinkcache_checksum_key UNIQUE (checksum);


--
-- Name: sinkcache_pkey; Type: CONSTRAINT; Schema: public; Owner: jobstore; Tablespace: 
--

ALTER TABLE ONLY sinkcache
    ADD CONSTRAINT sinkcache_pkey PRIMARY KEY (id);


--
-- Name: chunk_timeofcreationforunfinishedchunks_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX chunk_timeofcreationforunfinishedchunks_index ON chunk USING btree (timeofcreation) WHERE (timeofcompletion IS NULL);


--
-- Name: chunk_timeoflastmodification_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX chunk_timeoflastmodification_index ON chunk USING btree (timeoflastmodification);


--
-- Name: item_statefailed_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX item_statefailed_index ON item USING btree (jobid, chunkid, id) WHERE ((((((state -> 'states'::text) -> 'PARTITIONING'::text) ->> 'failed'::text) <> '0'::text) OR ((((state -> 'states'::text) -> 'PROCESSING'::text) ->> 'failed'::text) <> '0'::text)) OR ((((state -> 'states'::text) -> 'DELIVERING'::text) ->> 'failed'::text) <> '0'::text));


--
-- Name: item_stateignored_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX item_stateignored_index ON item USING btree (jobid, chunkid, id) WHERE ((((((state -> 'states'::text) -> 'PARTITIONING'::text) ->> 'ignored'::text) <> '0'::text) OR ((((state -> 'states'::text) -> 'PROCESSING'::text) ->> 'ignored'::text) <> '0'::text)) OR ((((state -> 'states'::text) -> 'DELIVERING'::text) ->> 'ignored'::text) <> '0'::text));


--
-- Name: job_deliveringfailed_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX job_deliveringfailed_index ON job USING btree (id) WHERE ((((state -> 'states'::text) -> 'DELIVERING'::text) ->> 'failed'::text) <> '0'::text);


--
-- Name: job_fatalerror_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX job_fatalerror_index ON job USING btree (id) WHERE (fatalerror = true);


--
-- Name: job_processingfailed_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX job_processingfailed_index ON job USING btree (id) WHERE ((((state -> 'states'::text) -> 'PROCESSING'::text) ->> 'failed'::text) <> '0'::text);


--
-- Name: job_specification_idx; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX job_specification_idx ON job USING gin (specification jsonb_path_ops);


--
-- Name: job_timeofcreation_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX job_timeofcreation_index ON job USING btree (timeofcreation);


--
-- Name: job_timeoflastmodification_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX job_timeoflastmodification_index ON job USING btree (timeoflastmodification);


--
-- Name: notification_jobid_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX notification_jobid_index ON notification USING btree (jobid);


--
-- Name: notification_status_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX notification_status_index ON notification USING btree (status);


--
-- Name: schema_version_ir_idx; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX schema_version_ir_idx ON schema_version USING btree (installed_rank);


--
-- Name: schema_version_s_idx; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX schema_version_s_idx ON schema_version USING btree (success);

--
-- Name: sink_reference_index; Type: INDEX; Schema: public; Owner: jobstore; Tablespace: 
--

CREATE INDEX sink_reference_index ON job USING btree ((((((flowstorereferences -> 'references'::text) -> 'SINK'::text) ->> 'id'::text))::integer));


--
-- Name: chunk_timeoflastmodification_trigger; Type: TRIGGER; Schema: public; Owner: jobstore
--

CREATE TRIGGER chunk_timeoflastmodification_trigger BEFORE UPDATE ON chunk FOR EACH ROW EXECUTE PROCEDURE update_timeoflastmodification();


--
-- Name: item_timeoflastmodification_trigger; Type: TRIGGER; Schema: public; Owner: jobstore
--

CREATE TRIGGER item_timeoflastmodification_trigger BEFORE UPDATE ON item FOR EACH ROW EXECUTE PROCEDURE update_timeoflastmodification();


--
-- Name: job_timeoflastmodification_trigger; Type: TRIGGER; Schema: public; Owner: jobstore
--

CREATE TRIGGER job_timeoflastmodification_trigger BEFORE UPDATE ON job FOR EACH ROW EXECUTE PROCEDURE update_timeoflastmodification();


--
-- Name: job_cachedflow_fkey; Type: FK CONSTRAINT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY job
    ADD CONSTRAINT job_cachedflow_fkey FOREIGN KEY (cachedflow) REFERENCES flowcache(id);


--
-- Name: job_cachedsink_fkey; Type: FK CONSTRAINT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY job
    ADD CONSTRAINT job_cachedsink_fkey FOREIGN KEY (cachedsink) REFERENCES sinkcache(id);


--
-- Name: jobqueue_jobid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY jobqueue
    ADD CONSTRAINT jobqueue_jobid_fkey FOREIGN KEY (jobid) REFERENCES job(id);


--
-- Name: notification_job_fkey; Type: FK CONSTRAINT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_job_fkey FOREIGN KEY (job) REFERENCES job(id) ON DELETE CASCADE;


--
-- Name: notification_jobid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: jobstore
--

ALTER TABLE ONLY notification
    ADD CONSTRAINT notification_jobid_fkey FOREIGN KEY (jobid) REFERENCES job(id);


