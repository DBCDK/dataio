CREATE INDEX CONCURRENTLY IF NOT EXISTS dependencytracking_waitingon_idx ON dependencytracking USING gin (waitingon jsonb_path_ops);
