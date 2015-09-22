ALTER TABLE job ALTER COLUMN specification TYPE JSONB USING specification::JSONB;
CREATE INDEX ON job USING GIN (specification jsonb_path_ops);
