
delete from taskpackage;
delete from esinflight;


CREATE or REPLACE FUNCTION create_test_package(tRef INTEGER) RETURNS void AS $$
--
--  One taskpackage which is  in progresss with on record done
--
  insert into taskpackage (packagetype, packageName, userid, targetReference, creator, taskstatus)
  values (5, 'test package '||$1::text, 2, $1, 'JTPWCopy test script', 0); -- 0 for task active

  insert into "taskspecificupdate" (targetReference, action, databaseName, schema, elementSetName, UpdateStatus)
  values ( $1, 3, 'dbname', 'schema1', 'elementSet1 ', 1); -- 1 for success


 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( $1, 0, 'SupplementalID1'::BYTEA , 'record 1'::BYTEA );

 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( $1, 1, 'SupplementalID2'::BYTEA , 'record 2'::BYTEA );

 update taskpackagerecordstructure set recordstatus=1 where lbnr=1 and targetreference = $1; -- 1 for record Success

insert into esinflight (targetreference, chunkid, jobid, chunk, sinkid, databasename)
       values (  $1, 0, 1, '{"jobId":6849,"chunkId":0,"type":"DELIVERED","items":[{"id":0,"data":"MQ==","status":"SUCCESS"},{"id":0,"data":"MQ==","status":"SUCCESS"}],"encoding":"ISO-8859-1"}', 1, 'database1');

  $$ LANGUAGE sql;


DO
$do$
DECLARE
 _counter int := 0;
BEGIN
WHILE _counter < 10
LOOP
   _counter := _counter + 1;

  perform create_test_package(_counter) ;

END LOOP;
END
$do$;

drop FUNCTION IF EXISTS create_test_package();

