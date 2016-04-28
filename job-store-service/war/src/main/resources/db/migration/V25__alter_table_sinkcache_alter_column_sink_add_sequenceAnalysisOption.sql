CREATE OR REPLACE FUNCTION insert_sequence_analysis_option_into_sinkcache_sink_content(sink JSON)
    RETURNS JSON AS $$
    sink.content.sequenceAnalysisOption='ALL'
    return sink
$$ LANGUAGE plv8 STABLE STRICT;

update sinkcache set sink = insert_sequence_analysis_option_into_sinkcache_sink_content(sink);
drop FUNCTION insert_sequence_analysis_option_into_sinkcache_sink_content(JSON);