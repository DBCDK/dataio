CREATE extension IF NOT EXISTS plv8;

CREATE OR REPLACE FUNCTION create_chunkItemFrom_itemEntity(id INT, outcome JSON, state JSON, step TEXT)
    RETURNS JSON AS $$
    if( outcome === null || outcome === undefined ) return 'null';

    var phase = state.states[step];
    var result = 'Unknown';
    for(key in phase) {
        if(phase[key] == 1) {
            switch(key) {
              case "succeeded":
                result = "SUCCESS";
                break;
              case "failed":
                result = "FAILURE";
                break;
              case "ignored":
                result = "IGNORE";
                break;
              default:
                plv8.elog(WARNING, "MIS on " + phase[key]);
            }
        }
    }
   return  {"id":id, "data":outcome.data, "status": result};
$$ LANGUAGE plv8 STABLE STRICT;

ALTER TABLE item ALTER COLUMN partitioningoutcome TYPE JSONB USING create_chunkItemFrom_itemEntity(id, partitioningoutcome, state, 'PARTITIONING')::JSONB,
                 ALTER COLUMN processingoutcome TYPE JSONB USING create_chunkItemFrom_itemEntity(id, processingoutcome, state, 'PROCESSING')::JSONB,
                 ALTER COLUMN deliveringoutcome TYPE JSONB USING create_chunkItemFrom_itemEntity(id, deliveringoutcome, state, 'DELIVERING')::JSONB,
                 ALTER COLUMN nextprocessingoutcome TYPE JSONB USING nextprocessingoutcome::JSONB;