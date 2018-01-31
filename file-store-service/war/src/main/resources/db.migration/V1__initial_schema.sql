CREATE TABLE file_attributes (
    id bigint NOT NULL,
    creationtime timestamp without time zone,
    location character varying(255),
    bytesize bigint,
    PRIMARY KEY(id)
);

CREATE TABLE sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0),
    PRIMARY KEY(seq_name)
);
