INSERT INTO dataset(name, agencyId, displayName) VALUES ('dataset', '123456', 'displayname');
INSERT INTO dataset(name, agencyId, displayName) VALUES ('viaf', '424242', 'VIAF');

INSERT INTO batch(dataset,batchkey,type,timeOfCompletion) VALUES (1, 1000001, 'TOTAL', now());
INSERT INTO batch(dataset,batchkey,type,timeOfCompletion) VALUES (1, 1000002, 'TOTAL', now());
INSERT INTO batch(dataset,batchkey,type,timeOfCompletion) VALUES (1, 1000003, 'TOTAL', now());
INSERT INTO batch(dataset,batchkey,type,timeOfCompletion) VALUES (2, 2000001, 'TOTAL', now());

INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (1, 1, 'id_1_1', 't_1_1', 'data_1_1', 'chksum_1_1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (2, 1, 'id_2_1', 't_2_1', 'data_2_1', 'chksum_2_1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (3, 1, 'id_3_1', 't_3_1', 'data_3_1', 'chksum_3_1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (3, 1, 'id_3_2', 't_3_2', 'data_3_2', 'chksum_3_2', 'DELETED');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (3, 1, 'id_3_3', 't_3_3', 'data_3_3', 'chksum_3_3', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (4, 2, 'viaf_1', 'track_viaf_1', '<marcx:record xmlns:marcx="info:lc/xmlns/marcxchange-v1"><marcx:leader>321</marcx:leader><marcx:datafield ind1="0" ind2="0" tag="001"><marcx:subfield code="a">viaf_1</marcx:subfield></marcx:datafield></marcx:record>', 'chksum_viaf_1', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (4, 2, 'viaf_2', 'track_viaf_2', 'not marcXchange', 'chksum_viaf_2', 'ACTIVE');
INSERT INTO record(batch,dataset,localid,trackingid,content,checksum,status) VALUES (4, 2, 'viaf_3', 'track_viaf_3', '<marcx:record xmlns:marcx="info:lc/xmlns/marcxchange-v1"><marcx:leader>321</marcx:leader><marcx:datafield ind1="0" ind2="0" tag="001"><marcx:subfield code="a">viaf_3</marcx:subfield></marcx:datafield></marcx:record>', 'chksum_viaf_3', 'DELETED');
