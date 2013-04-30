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

/*
Until we figure out how to have the JPA layer add a CAST we need to execute the following in the database:

CREATE CAST (varchar AS json) WITHOUT FUNCTION AS IMPLICIT;

The WITHOUT FUNCTION clause is used because we know that text and json have the same on-disk and in-memory
representation, they're basically just aliases for the same data type. (This may not always remain the case,
but is true up to at least PostgreSQL 9.3). AS IMPLICIT tells PostgreSQL it can convert without being 
explicitly told to.
*/
