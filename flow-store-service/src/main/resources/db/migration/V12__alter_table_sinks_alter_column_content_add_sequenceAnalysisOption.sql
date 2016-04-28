CREATE EXTENSION IF NOT EXISTS plv8;
CREATE OR REPLACE FUNCTION insert_sequence_analysis_option_into_sinks_content(content JSON)
    RETURNS JSON AS $$
    content.sequenceAnalysisOption='ALL'
    return content
$$ LANGUAGE plv8 STABLE STRICT;

UPDATE sinks SET content = insert_sequence_analysis_option_into_sinks_content(content::JSON)::JSONB;
DROP FUNCTION insert_sequence_analysis_option_into_sinks_content(JSON);