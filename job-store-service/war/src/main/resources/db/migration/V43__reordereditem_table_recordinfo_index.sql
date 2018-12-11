CREATE INDEX ON reordereditem USING GIN (recordinfo jsonb_path_ops);
