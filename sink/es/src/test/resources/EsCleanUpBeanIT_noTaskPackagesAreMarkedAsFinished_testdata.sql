
delete from taskpackage;
delete from esinflight;


--
--  One taskpackage which is  in progresss with on record done
--
insert into taskpackage (packagetype, packageName, userid, targetReference, creator, taskstatus)
       values (5, 'test package 1', 2, nextval('taskpackagerefseq'), 'JTPWCopy test script', 1); -- 1 for task active

 insert into "taskspecificupdate" (targetReference, action, databaseName, schema, elementSetName, UpdateStatus)
      values ( currval('taskpackagerefseq'), 3, 'dbname', 'schema1', 'elementSet1 ', 1); -- 1 for success


 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( currval('taskpackagerefseq'), 0, 'SupplementalID1'::BYTEA , 'record 1'::BYTEA );

 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( currval('taskpackagerefseq'), 1, 'SupplementalID2'::BYTEA , 'record 2'::BYTEA );

 update taskpackagerecordstructure set recordstatus=1 where lbnr=1 and targetreference = currval('taskpackagerefseq'); -- 1 for record Success

insert into esinflight (targetreference, resourcename, chunkid, jobid, recordslots, sinkchunkresult)
       values (  currval('taskpackagerefseq'), 'test/resource', 0, 1, 3, '{"jobId":6849,"chunkId":0,"type":"DELIVERED","items":[{"id":0,"data":"MQ==","status":"SUCCESS"},{"id":0,"data":"MQ==","status":"SUCCESS"}],"encoding":"ISO-8859-1"}');


--
-- One task package pending
--

insert into taskpackage (packagetype, packageName, userid, targetReference, creator, taskstatus)
       values (5, 'test package 2', 2, nextval('taskpackagerefseq'), 'JTPWCopy test script', 0); -- 0 for pending for task complete

 insert into "taskspecificupdate" (targetReference, action, databaseName, schema, elementSetName )
      values ( currval('taskpackagerefseq'), 3, 'dbname', 'schema1', 'elementSet1 ');


 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( currval('taskpackagerefseq'), 0, 'SupplementalID1'::BYTEA , 'record 1'::BYTEA );


insert into esinflight (targetreference, resourcename, chunkid, jobid, recordslots, sinkchunkresult)
       values (  currval('taskpackagerefseq'), 'test/resource', 0, 1, 3, '{"jobId":6849,"chunkId":0,"type":"DELIVERED","items":[{"id":0,"data":"MQ==","status":"SUCCESS"}],"encoding":"ISO-8859-1"}');