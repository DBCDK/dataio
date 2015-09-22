CREATE INDEX sink_reference_index ON job(((flowstorereferences->'references'->'SINK'->>'id')::INT));
