delete from job;
delete from sinkcache;

insert into sinkcache (id, checksum, sink ) values ( 0 , '927d164ba5baedff9e54cdb9a81fc5ce', '{"id":4,"version":3,"content":{"name":"FBS","resource":"url/dataio/fbs/ws","sequenceAnalysisOption":"ALL"}}'::JSON );


insert into job (id, specification, state, flowstorereferences) values (1,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, specification, state, flowstorereferences) values (2,'{}'::JSONB, '{}'::JSON, '{}'::JSON);
insert into job (id, cachedsink, specification, state, flowstorereferences) values (3,0, '{"type": "TRANSIENT", "format": "basis", "charset": "utf8", "dataFile": "urn:dataio-fs:55956", "packaging": "xml", "destination": "broend3-exttest", "submitterId": 870970, "resultmailInitials": "", "mailForNotificationAboutProcessing": "", "mailForNotificationAboutVerification": ""}'::JSONB, '{"states":{"DELIVERING":{"beginDate":null,"endDate":null,"succeeded":0,"failed":0,"ignored":0},"PARTITIONING":{"beginDate":1433751706618,"endDate":null,"succeeded":10,"failed":0,"ignored":0},"PROCESSING":{"beginDate":null,"endDate":null,"succeeded":0,"failed":0,"ignored":0}}}
'::JSON, '{"references":{"SINK":{"id":752,"version":2,"name":"RR2Brøndtest - ender den i manam?"},"FLOW_BINDER":{"id":755,"version":4,"name":"RR2Brønd 2 - Basisposter"},"SUBMITTER":{"id":6,"version":1,"name":"Basis"},"FLOW":{"id":9,"version":9,"name":"RR2Brønd"}}}
'::JSON);


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

    _counter := _counter + 1;


END LOOP;
END
$do$;


insert into item ( jobid, chunkid, id, state, processingoutcome ) values
    ( 3,1,0, '{}'::JSON, '{"id": 0, "data": "SGVsbG8gZnJvbSBqYXZhc2NyaXB0IQo=", "type": ["UNKNOWN"], "status": "SUCCESS", "encoding": "UTF-8", "trackingId": "172.20.1.191-63087-0-0"}'::JSONB);

INSERT INTO dependencytracking (jobid, chunkid, sinkid, status, waitingon, blocking, matchkeys) VALUES (3, 1, 1, 4, '[{"jobId": 3, "chunkId": 0}]', null, '["K8", "KK2", "C4"]');



