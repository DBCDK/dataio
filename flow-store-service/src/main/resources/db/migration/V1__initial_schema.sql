-- 
-- DataIO - Data IO
-- Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
-- Denmark. CVR: 15149043
--
-- This file is part of DataIO.
--
-- DataIO is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- DataIO is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
-- 

--
-- flows
CREATE TABLE flows (
    id bigint NOT NULL PRIMARY KEY,
    content text NOT NULL,
    name_idx text NOT NULL UNIQUE,
    version bigint NOT NULL
);
CREATE INDEX index_flows_name_idx ON flows USING btree (name_idx);


--
-- Sinks
CREATE TABLE sinks (
    id bigint NOT NULL PRIMARY KEY ,
    content text NOT NULL,
    name_idx text NOT NULL UNIQUE ,
    version bigint NOT NULL
);

--
-- submitters
CREATE TABLE submitters (
    id bigint NOT NULL PRIMARY KEY ,
    content text NOT NULL,
    name_idx text NOT NULL UNIQUE ,
    number_idx bigint NOT NULL UNIQUE ,
    version bigint NOT NULL
);

--
-- flow_binders
CREATE TABLE flow_binders (
    id bigint NOT NULL,
    content text NOT NULL,
    name_idx text NOT NULL UNIQUE ,
    version bigint NOT NULL,
    flow_id bigint REFERENCES flows(id),
    sink_id bigint REFERENCES sinks(id),
    PRIMARY KEY (id)
);


CREATE TABLE flow_binders_search_index (
    packaging text NOT NULL,
    submitter_number bigint NOT NULL,
    charset text NOT NULL,
    format text NOT NULL,
    destination text NOT NULL,
    flow_binder_id bigint REFERENCES flow_binders(id),
    PRIMARY KEY (packaging, submitter_number, charset, format, destination)
);

--
-- Name: flow_binders_submitters; Type: TABLE; Schema: public; Owner: flowstore; Tablespace: 
--

CREATE TABLE flow_binders_submitters (
    flow_binder_id bigint NOT NULL,
    submitter_id bigint NOT NULL,
    FOREIGN KEY (flow_binder_id) REFERENCES flow_binders(id),
    FOREIGN KEY (submitter_id) REFERENCES submitters(id),
    PRIMARY KEY (flow_binder_id, submitter_id)
);

--
-- Name: flow_components; Type: TABLE; Schema: public; Owner: flowstore; Tablespace: 
--

CREATE TABLE flow_components (
    id bigint NOT NULL PRIMARY KEY,
    content text NOT NULL,
    name_idx text NOT NULL UNIQUE,
    version bigint NOT NULL
);

CREATE INDEX index_flow_components_name_idx ON flow_components USING btree (name_idx);

--
-- Name: flows; Type: TABLE; Schema: public; Owner: flowstore; Tablespace: 
--

CREATE TABLE sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0),
    PRIMARY KEY (seq_name)
);


insert into sequence ( seq_name, seq_count ) values ('SEQ_GEN',1200);
--
-- Name: sinks; Type: TABLE; Schema: public; Owner: flowstore; Tablespace: 
--

--

