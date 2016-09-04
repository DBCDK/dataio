ALTER TABLE flows ALTER COLUMN content TYPE JSONB USING content::JSONB;
alter table flows drop COLUMN name_idx;
create unique index on flows((content->>'name'));