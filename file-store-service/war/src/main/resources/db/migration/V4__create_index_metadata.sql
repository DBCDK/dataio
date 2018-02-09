CREATE INDEX metadata_index ON file_attributes USING GIN(metadata jsonb_path_ops);
