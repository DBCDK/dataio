delete from job;

insert into job (id, specification, state, flowstorereferences) values (1,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (2,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences)
values (3,
        '{"type": "TEST", "format": "b", "charset": "utf8", "ancestry": null, "dataFile": "df", "packaging": "p", "destination": "d", "submitterId": 870970, "resultmailInitials": "", "mailForNotificationAboutProcessing": "", "mailForNotificationAboutVerification": ""}'::JSONB,
        '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (4,'{}'::JSONB, '{}'::JSON, '{}'::JSON);


--
-- Create lots of chunks for testing
--
DO
$do$
DECLARE
 _counter int := 0;
BEGIN
WHILE _counter < 5
LOOP
   insert into chunk (jobid, id, datafileid, state) values (1,_counter,'','{}'::JSON);
   insert into chunk (jobid, id, datafileid, state) values (2,_counter,'','{}'::JSON);
   insert into chunk (jobid, id, datafileid, state) values (3,_counter,'','{}'::JSON);
   insert into chunk (jobid, id, datafileid, state) values (4,_counter,'','{}'::JSON);
    _counter := _counter + 1;

END LOOP;
END
$do$;


insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (1, 0, 0, 123456, 2);
insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (1, 1, 0, 123456, 2);
insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (1, 2, 0, 123456, 2);
insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (1, 3, 0, 123456, 2);

insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (2, 0, 1, 123456, 2);
insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (2, 1, 1, 123456, 2);
insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (2, 2, 1, 123456, 2);
insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (2, 3, 1, 123456, 2);
insert into dependencytracking (jobid, chunkid, sinkid, submitter, status) values (2, 4, 1, 123456, 2);

insert into dependencytracking (jobid, chunkid, sinkid, status) values (4, 0, 1, 2);
insert into dependencytracking (jobid, chunkid, sinkid, status) values (4, 1, 1, 2);


