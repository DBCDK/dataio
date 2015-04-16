
Readme
======

Dette katalog indeholder developer scripts til at sætte en eksterne glassfish op,
så den kan bruges til udvikling på en lokal maskine.

Postgresql
==========

vi skal bruge en del postgresql databaser.

debian/ubuntu
-------------

Lav en bruger til alle databaserne.

.. code-block::
 createuser -DRS dataio
..


createtables
------------
                    <param>${basedir}/../../new-job-store-service/resources/schema/jobstore_schema_pg.sql</param>
                    <param>${basedir}/../../log-store-service/dbhelper/src/main/resources/logstore-schema.sql</param>


python tools
------------

create_job.py  -- opretter et job.


resource-files
--------------

de load's med

asadmin add-resources ./localhost-pg-resorces-2.xml