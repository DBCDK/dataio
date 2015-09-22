CREATE FUNCTION dbc_jsonb_append(jsonb, jsonb)
   RETURNS jsonb AS $$
     WITH json_union AS
       (SELECT * FROM jsonb_each_text($1)
          UNION ALL
        SELECT * FROM jsonb_each_text($2))
     SELECT json_object_agg(key, value)::jsonb FROM json_union;
   $$ LANGUAGE SQL;

CREATE FUNCTION dbc_jsonb_append_key_value(jsonb, text, text)
   RETURNS jsonb as $$
     SELECT dbc_jsonb_append($1, json_build_object($2, $3)::jsonb);
   $$ LANGUAGE SQL;

CREATE FUNCTION dbc_jsonb_append_key_value_pairs(jsonb, variadic text[])
   RETURNS jsonb AS $$
     SELECT dbc_jsonb_append($1, json_object($2)::jsonb);
   $$ LANGUAGE SQL;
