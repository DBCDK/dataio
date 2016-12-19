INSERT INTO dataset(name, agencyId, displayName) VALUES ('dataset1', '123456', 'displayname1');

INSERT INTO batch(dataset,batchkey,type) VALUES (1, 1000001, 'TOTAL');

INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (1, 1, 'id1', 't1', 'old1', 'chksum1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (1, 1, 'id2', 't2', 'old2', 'oldChksum2', 'DELETED');