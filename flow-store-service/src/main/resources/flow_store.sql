CREATE or replace FUNCTION json_text_member (object json, key text )
    RETURNS text
    LANGUAGE plv8
    IMMUTABLE
AS $function$

var ej = JSON.parse(object);
if (typeof ej != 'object')
    return NULL;
return ej[key];

$function$;

CREATE TABLE flows (
    id SERIAL NOT NULL PRIMARY KEY,
    data json
);

CREATE UNIQUE INDEX flows_flowname_index ON flows (json_text_member(data,'flowname'));


