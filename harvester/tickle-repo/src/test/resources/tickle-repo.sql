INSERT INTO dataset(name, agencyId, displayName) VALUES ('dataset', '123456', 'displayname');

INSERT INTO batch(dataset,batchkey,type,timeOfCompletion) VALUES (1, 1000001, 'TOTAL', now());
INSERT INTO batch(dataset,batchkey,type,timeOfCompletion) VALUES (1, 1000002, 'TOTAL', now());
INSERT INTO batch(dataset,batchkey,type,timeOfCompletion) VALUES (1, 1000003, 'TOTAL', now());

INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (1, 1, 'id_1_1', 't_1_1', 'data_1_1', 'chksum_1_1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (2, 1, 'id_2_1', 't_2_1', 'data_2_1', 'chksum_2_1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (3, 1, 'id_3_1', 't_3_1', 'data_3_1', 'chksum_3_1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (3, 1, 'id_3_2', 't_3_2', 'data_3_2', 'chksum_3_2', 'DELETED');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (3, 1, 'id_3_3', 't_3_3', 'data_3_3', 'chksum_3_3', 'ACTIVE');
