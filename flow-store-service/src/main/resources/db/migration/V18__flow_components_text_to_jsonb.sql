ALTER TABLE flow_components ALTER COLUMN content TYPE JSONB USING content::JSONB;
alter TABLE flow_components ALTER COLUMN next type JSONB using next::JSONB;
alter table flow_components drop COLUMN name_idx;
create unique index on flow_components((content->>'name'));