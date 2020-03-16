ALTER TABLE flow_components ADD COLUMN view JSONB DEFAULT NULL;
CREATE INDEX flow_components_view_index ON flow_components USING GIN (view jsonb_path_ops);
