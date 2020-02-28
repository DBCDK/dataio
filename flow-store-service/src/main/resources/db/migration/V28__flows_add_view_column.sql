ALTER TABLE flows ADD COLUMN view JSONB DEFAULT NULL;
CREATE INDEX flows_view_index ON flows USING GIN (view jsonb_path_ops);
