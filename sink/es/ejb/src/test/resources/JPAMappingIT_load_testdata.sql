
delete from taskpackage;

delete from esinflight;

--
-- taskpackage 1 which is  in progresss with on record done
--
insert into taskpackage (packagetype, packageName, userid, targetReference, creator, taskstatus)
       values (5, 'test package 1', 2, 1, 'JTPWCopy test script', 1); -- 1 for task active

 insert into "taskspecificupdate" (targetReference, action, databaseName, schema, elementSetName, UpdateStatus)
      values ( 1, 3, 'dbname', 'schema1', 'elementSet1 ', 1); -- 1 for success


 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( 1, 0, 'SupplementalID1'::BYTEA , 'record 1'::BYTEA );

 INSERT INTO suppliedrecords (targetreference, lbnr, SUPPLEMENTALID3, record)
        VALUES ( 1, 1, 'SupplementalID2'::BYTEA , 'record 2'::BYTEA );

update taskpackagerecordstructure set recordstatus=1 where lbnr=1 and targetreference = 1; -- 1 for record Success

UPDATE taskpackagerecordstructure set (recordstatus,recordorsurdiag2)=(2,nextval('diagidseq'))  where lbnr=1 and targetreference = 1;

insert into diagnostics (id, lbnr, diagnosticsetid, condition, addinfo) values ( currval('diagidseq'), 0, '1.2', 42, 'diag1' );
insert into diagnostics (id, lbnr, diagnosticsetid, condition, addinfo) values ( currval('diagidseq'), 1, '1.2', 100, 'diag2' );




