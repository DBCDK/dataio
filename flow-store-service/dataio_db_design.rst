DataIO flow store database design
=================================

Vi ønsker et PostgreSQL database design inspireret af NoSQL "document store" tankegangen, hvor vi kan anskue de enkelte tabeller som dokument samlinger med data repræsenteret som JSON tekst blobs.

Nyere versioner af PostgreSQL giver os forskellige muligheder for at implementere dette:

* v9.1 JSON kan repræsenteres som kolonner af typen TEXT, og fleksibel indeks håndtering kræver at PLV8 extension er installeret.

* v9.2 JSON kan repræsenteres som kolonner af typen JSON, hvilket giver basal JSON validering under indsættelse, men som stadig kræver at PLV8 extension er installeret for fleksibel indeks håndtering.

* v9.3 JSON kan repræsenteres som kolonner at typen JSON, og et rigt data-access sprog gør at PLV8 extension højst sandsynlig ikke er nødvendig.

Vi regner med at have et drift miljø, som kører PostgreSQL v9.2

Schema eksempel
---------------

::

    CREATE TABLE flows (
        id SERIAL NOT NULL PRIMARY KEY,
        data json
    );

Simpel stored procedure (i Javascript) til at hente tekst værdier ud::

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

Det vil nu være muligt at danne indekser og constraints på tekst værdier i JSON objektet, ekempelvis::

    CREATE UNIQUE INDEX flows_flowname_index ON flows (json_text_member(data,'flowname'));

Ekempel på data indsættelse og selektering::

    INSERT INTO flows(data) VALUES('{"flowname":"flow1","description":"this is my first flow"}');
    SELECT json_text_member(data, 'description') FROM flows;

