
delete from taskpackage;
delete from esinflight;

insert into userids values ( 2, 1000 );
insert into userids values ( 3, 1000 );

INSERT INTO databases (databasename, max_update_priority,  z3950_init_timeout, z3950_es_timeout) VALUES ('dbname', 1000, 0, 0);
