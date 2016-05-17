CREATE TABLE harvester_config (
    id bigint NOT NULL PRIMARY KEY,
    version bigint NOT NULL,
    type TEXT,
    content JSONB
);

create index harvester_config_content on harvester_config using GIN ( content jsonb_path_ops);
