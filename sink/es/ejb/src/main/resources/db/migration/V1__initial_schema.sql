CREATE TABLE esinflight (
    targetreference integer NOT NULL,
    resourcename text NOT NULL,
    chunkid bigint NOT NULL,
    jobid bigint NOT NULL,
    recordslots integer NOT NULL,
    sinkchunkresult text NOT NULL
);

ALTER TABLE ONLY esinflight
    ADD CONSTRAINT esinflight_pkey PRIMARY KEY (targetreference, resourcename);