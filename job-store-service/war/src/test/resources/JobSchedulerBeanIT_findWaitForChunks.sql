delete from job;

insert into job (id, specification, state, flowstorereferences) values (1,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (2,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (3,'{}'::JSONB, '{}'::JSON, '{}'::JSON);


--
-- Create lots of chunks for testing
--
DO
$do$
DECLARE
 _counter int := 0;
BEGIN
WHILE _counter < 30
LOOP
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (1,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (2,_counter,'','{}'::JSON,'{}'::JSON);
    _counter := _counter + 1;

END LOOP;
END
$do$;


INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (1, 0, 0, 2, null, null, '["K0", "KK2", "C0"]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (1, 1, 0, 2, null, null, '["K1", "KK2", "C1"]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (1, 2, 0, 2, null, null, '["K2", "KK2", "C2"]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (1, 3, 0, 2, null, null, '["K3", "KK2", "C2"]');

INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (2, 0, 1, 2, null, null, '["K4", "KK2", "C0"]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (2, 1, 1, 2, null, null, '["K5", "KK2", "C1"]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (2, 2, 1, 2, null, null, '["K6", "KK2", "C2"]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (2, 3, 1, 2, null, null, '["K7", "KK2", "C3"]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (2, 4, 1, 2, null, null, '["K8", "KK2", "C4"]');



