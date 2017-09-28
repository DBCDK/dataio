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
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (1,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (2,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (3,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (4,_counter,'','{}'::JSON,'{}'::JSON);
    _counter := _counter + 1;

END LOOP;
END
$do$;


INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 0, 0, 2, '[{"jobId": 3, "chunkId": 0}]', '["K0", "KK2", "C0"]', '{2373,74450,2125}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 1, 0, 2, '[{"jobId": 3, "chunkId": 0}]', '["K1", "KK2", "C1"]', '{2374,74450,2126}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 2, 0, 2, '[{"jobId": 3, "chunkId": 0}]', '["K2", "KK2", "C2"]', '{2375,74450,2127}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 3, 0, 2, '[{"jobId": 3, "chunkId": 0}]', '["K3", "KK2", "C3"]', '{2376,74450,2128}');

INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 0, 1, 2, '[{"jobId": 3, "chunkId": 0}]', '["K4", "KK2", "C0"]', '{2377,74450,2125}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 1, 1, 2, '[{"jobId": 3, "chunkId": 0}]', '["K5", "KK2", "C1"]', '{2378,74450,2126}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 2, 1, 2, '[{"jobId": 3, "chunkId": 0}]', '["K6", "KK2", "C2"]', '{2379,74450,2127}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 3, 1, 2, '[{"jobId": 3, "chunkId": 0}]', '["K7", "KK2", "C3"]', '{2380,74450,2128}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 4, 1, 2, '[{"jobId": 3, "chunkId": 0}]', '["K8", "KK2", "C4"]', '{2381,74450,2129}');

INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (4, 0, 1, 2, '[]', '["4_0"]', '{52965}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (4, 1, 1, 2, '[{"jobId": 4, "chunkId": 0}]', '["4_0", "4_1"]', '{52965,52966}');


