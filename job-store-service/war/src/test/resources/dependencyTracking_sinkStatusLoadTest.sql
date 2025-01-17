
delete from job;

-- create som jobs
insert into job (id, specification, state, flowstorereferences) values (1,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (2,'{}'::JSONB, '{}'::JSON, '{}'::JSON);

DO
$do$
DECLARE
 _counter int := 0;
BEGIN
WHILE _counter < 20
LOOP
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (1,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (2,_counter,'','{}'::JSON,'{}'::JSON);
    _counter := _counter + 1;
END LOOP;
END
$do$;



INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 0,  1551, 1, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 1,  1551, 2, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 2,  1551, 2, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 3,  1551, 3, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 4,  1551, 3, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 5,  1551, 3, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 6,  1551, 4, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 8,  1551, 4, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 9,  1551, 4, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 10, 1551, 4, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 11, 1551, 5, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 12, 1551, 5, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 13, 1551, 5, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 14, 1551, 5, '[]', '[]');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (1, 15, 1551, 5, '[]', '[]');


INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 0,  1, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 1,  1, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 2,  1, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 3,  1, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 4,  1, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 5,  1, 2, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 6,  1, 2, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 8,  1, 2, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 9,  1, 2, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 10, 1, 3, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 11, 1, 3, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 12, 1, 4, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 13, 1, 4, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 14, 1, 4, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, matchkeys, hashes) VALUES (2, 15, 1, 5, '[]', '[]', '{}');












