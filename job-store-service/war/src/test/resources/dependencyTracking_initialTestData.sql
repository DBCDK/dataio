
delete from job;

-- create some jobs
insert into job (id, specification, state, flowstorereferences) values (1,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (2,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (3,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (4,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (5,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (6,'{}'::JSONB, '{}'::JSON, '{}'::JSON);

DO
$do$
DECLARE
 _counter int := 0;
BEGIN
WHILE _counter < 10
LOOP
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (1,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (2,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (3,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (4,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (5,_counter,'','{}'::JSON,'{}'::JSON);
   insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (6,_counter,'','{}'::JSON,'{}'::JSON);
    _counter := _counter + 1;
END LOOP;
END
$do$;

insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (1,20,'','{}'::JSON,'{}'::JSON);
insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (1,21,'','{}'::JSON,'{}'::JSON);
insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (6,20,'','{}'::JSON,'{}'::JSON);
insert into chunk (jobid, id, datafileid,sequenceanalysisdata, state) values (6,21,'','{}'::JSON,'{}'::JSON);

INSERT INTO dependencytracking (jobid, chunkid, sinkid, priority, status, waitingon, matchkeys, hashes) VALUES (1, 21,  4242, 4, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, priority, status, waitingon, matchkeys, hashes) VALUES (1, 20,  4242, 4, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, priority, status, waitingon, matchkeys, hashes) VALUES (6, 20,  4242, 4, 1, '[]', '[]', '{}');
INSERT INTO dependencytracking (jobid, chunkid, sinkid, priority, status, waitingon, matchkeys, hashes) VALUES (6, 21,  4242, 7, 1, '[]', '[]', '{}');
