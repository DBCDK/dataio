DROP FUNCTION IF EXISTS remove_all();

CREATE FUNCTION remove_all() RETURNS void AS $$
DECLARE
    rec RECORD;
    cmd text;
BEGIN
    cmd := '';

    FOR rec IN SELECT
            'DROP SEQUENCE IF EXISTS ' || quote_ident(n.nspname) || '.'
                || quote_ident(c.relname) || ' CASCADE;' AS name
        FROM
            pg_catalog.pg_class AS c
        LEFT JOIN
            pg_catalog.pg_namespace AS n
        ON
            n.oid = c.relnamespace
        WHERE
            relkind = 'S' AND
            n.nspname NOT IN ('pg_catalog', 'pg_toast') AND
            pg_catalog.pg_table_is_visible(c.oid)
    LOOP
        cmd := cmd || rec.name;
    END LOOP;

    FOR rec IN SELECT
            'DROP TABLE IF EXISTS ' || quote_ident(n.nspname) || '.'
                || quote_ident(c.relname) || ' CASCADE;' AS name
        FROM
            pg_catalog.pg_class AS c
        LEFT JOIN
            pg_catalog.pg_namespace AS n
        ON
            n.oid = c.relnamespace WHERE relkind = 'r' AND
            n.nspname NOT IN ('pg_catalog', 'pg_toast') AND
            pg_catalog.pg_table_is_visible(c.oid)
    LOOP
        cmd := cmd || rec.name;
    END LOOP;
  
    cmd := cmd || 'DROP EXTENSION IF EXISTS intarray CASCADE;';

    FOR rec IN SELECT
            'DROP FUNCTION IF EXISTS ' || quote_ident(ns.nspname) || '.'
                || quote_ident(proname) || '(' || oidvectortypes(proargtypes)
                || ');' AS name
        FROM
            pg_proc
        INNER JOIN
            pg_namespace ns
        ON
            (pg_proc.pronamespace = ns.oid)
        WHERE
            ns.nspname =
            'public'
        ORDER BY
            proname
    LOOP
        cmd := cmd || rec.name;
    END LOOP;

    FOR rec IN SELECT
            'DROP TYPE If EXISTS ' || quote_ident(n.nspname) || '.'
                            || quote_ident(t.typname)
                      || ' CASCADE ;' AS name
               FROM
                    pg_type t
               LEFT JOIN
                    pg_catalog.pg_namespace n
               ON
                    n.oid = t.typnamespace
               WHERE (t.typrelid = 0
                      OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid)
                     )
                AND  NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid)
                AND  n.nspname NOT IN ('pg_catalog', 'information_schema')
    LOOP
            cmd := cmd || rec.name;
    END LOOP;


    EXECUTE cmd;
    RETURN;
END;
$$ LANGUAGE plpgsql;

DO
$do$
BEGIN
  PERFORM remove_all();
END
$do$;
