/*
Index supporting the sink's lookup of the batches already staged for one record.

Every batch name begins with '<sinkId>-<recordKey>-', so the lookup is a prefix match.
text_pattern_ops is what lets a btree serve LIKE 'prefix%' under a non-C collation, and
the lookup runs once per delivered item, which a sequential scan of batch could not carry.
*/

CREATE INDEX IF NOT EXISTS batch_name_pattern_index ON batch (name text_pattern_ops);
