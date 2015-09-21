-- 
-- DataIO - Data IO
-- Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
-- Denmark. CVR: 15149043
--
-- This file is part of DataIO.
--
-- DataIO is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- DataIO is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
-- 
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
