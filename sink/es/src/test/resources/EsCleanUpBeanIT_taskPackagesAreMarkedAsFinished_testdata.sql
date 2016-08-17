
delete from taskpackage;
delete from esinflight;

insert into taskpackage (packagetype, packageName, userid, targetReference, creator, taskstatus)
       values (5, 'test package 1', 2, nextval('taskpackagerefseq'), 'JTPWCopy test script', 2); -- 2 for task complete

 insert into "taskspecificupdate" (targetReference, action, databaseName, schema, elementSetName, UpdateStatus)
      values ( currval('taskpackagerefseq'), 3, 'dbname', 'schema1', 'elementSet1 ', 1); -- 1 for success


 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( currval('taskpackagerefseq'), 0, 'SupplementalID1'::BYTEA , 'record 1'::BYTEA );

 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( currval('taskpackagerefseq'), 1, 'SupplementalID2'::BYTEA , 'record 2'::BYTEA );

 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( currval('taskpackagerefseq'), 2, 'SupplementalID3'::BYTEA , 'record 3'::BYTEA );

update taskpackagerecordstructure set recordstatus=1 where targetreference = currval('taskpackagerefseq'); -- 1 for record Success

insert into esinflight (targetreference, chunkid, jobid, chunk, sinkid, databasename)
       values (  currval('taskpackagerefseq'), 0, 1, '{"jobId":6849,"chunkId":0,"type":"DELIVERED","items":[{"id":0,"data":"MQ==","status":"SUCCESS", "type":["UNKNOWN"],"encoding":"UTF-8"}],"encoding":"ISO-8859-1"}', 1, 'database1');