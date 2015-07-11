
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
Eksempel: 
create_job.py --host dataio-be-s01:8080 testdata/tracebullet.xml testdata/tracebullet.jobspecification.json

list_job.py list job data from job store
export_job.py exports the job specification and job data
rerun_job.py resubmits a job as a new job.

resource-files
--------------

de load's med

asadmin add-resources ./localhost-pg-resorces-2.xml
