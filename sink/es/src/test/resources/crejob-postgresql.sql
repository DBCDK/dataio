/* Supprecs notices for existing tables */
SET client_min_messages TO WARNING;

create table IF NOT EXISTS userids (
userId numeric not null constraint pk_userids primary key,
update_priority numeric not null default 100
)
;

create table IF NOT EXISTS userid_priority (
userId numeric constraint ref_userid_priority references userids(userId),
constraint pk_userid_priority primary key (userId),
periodicQuery_priority numeric, -- hvis null k�res jobs ikke - ellers jo lavere tal - jo f�r
ItemOrder_priority numeric,
Update_priority numeric,
export_priority numeric
);

create table IF NOT EXISTS Databases (
DatabaseName varchar(200) not null constraint pk_Databases primary key,
max_update_priority numeric not null default 1000, --  kun tp's, hvor userid's update_priority er mindre end denne her kommer i betragtning
biblioteksnr numeric(6), -- patch dette nr i 001*b udfra TP's databasename
z3950_target varchar(80), -- TPW4wd taskpackage til denne target
z3950_user varchar(25), -- N�r TPW4wd connecter til target logges ind med
Z3950_group varchar(20), -- dette user, group, password
z3950_password varchar(20),
tilbase varchar(10), -- hvilken libV3-base skal TP opdateres i
z3950_init_timeout numeric(2) default 3 not null,
z3950_es_timeout numeric(3) default 3 not null -- timeout p� update request'et
)
;

create sequence taskpackageRefSeq;


create table taskpackage (
targetReference numeric constraint pk_taskpackage primary key, --  l�benr fra taskpackageRefSeq
taskstatus numeric(1) default 0 not null, --  0=pending 1=active 2=complete 3=aborted
constraint tp_taskstatus check (taskstatus in (0,1,2,3)),
substatus numeric(2) default 0 not null,
userId numeric constraint ref_userid references userids(userId) on delete cascade,
packageType numeric(1) not null, --  1=PersistentResultSet 2=PersistentQuery 3=PeriodicQuerySchedule 4=ItemOrder 5=Database Update 6=ExportSpecification 7=ExportInvocation
constraint tp_packagetype check (packageType in (1,2,3,4,5,6,7)),
packageName varchar(200),
creationdate date default 'now'::timestamp not null,
accessdate date default 'now'::timestamp not null,
retentiontime numeric(3) default 0 not null, --  antal dage efter accessdate hvor pkg slettes
creator varchar(200) not null, --  hvem har oprettet denne record testnep...
description varchar(200),
packagediagnostics numeric,
constraint taskpackage_triple unique (packageName, userId, packageType))
;

create sequence diagIdSeq;

create table diagnostics (
id numeric,	--  løbenr fra diagIdSeq
lbnr numeric not null,
diagnosticSetId varchar(200) not null, --  Object Identifier
condition numeric not null,
addinfo varchar(2000),
constraint pk_diagnostics primary key (id, lbnr))
;


create or replace function do_del_diag_taskpackage() returns trigger
LANGUAGE plpgsql
AS
$BODY$
begin
	delete from diagnostics
	where id=OLD.packagediagnostics;
  return old;
end;
$BODY$
;

DROP TRIGGER IF EXISTS delete_diagnostics ON taskpackage;
create trigger delete_diagnostics
after delete on taskpackage
for each row execute procedure do_del_diag_taskpackage()
;

create table taskspecificUpdate (
TargetReference numeric constraint ref_taskspecupdate references taskpackage(targetReference) on delete cascade,
constraint pk_taskspecificUpdate primary key (TargetReference),
Action numeric(1) not null, --  1=insert 2=replace 3=delete 4=elementUpdate 5=specialUpdate
 --constraint tp_upd_action check (Action in (1,2,3,4,5)),
DatabaseName varchar(200) constraint ref_tpupdate_references references Databases(DatabaseName),
schema varchar(200),
elementSetName varchar(200),
actionQualifier bytea,
UpdateStatus numeric(1) default 0 not null, --  1=success 2=partial 3=failure
constraint tp_upd_updstatus check (UpdateStatus in (0,1,2,3)),
noofrecs numeric default 0 not null,
noofrecs_treated numeric default 0 not null,
timetotreat numeric default 0 not null, --  hvor l�nge tog det at udf�re denne tp
globalDiagnostics numeric)
;

create or replace function do_del_diag_taskspecificUpdate() returns trigger
LANGUAGE plpgsql
AS
$BODY$
begin
	delete from diagnostics
	where id=OLD.globalDiagnostics;
  return old;
end;
$BODY$
;


DROP TRIGGER IF EXISTS delete_diagnostics ON taskspecificUpdate;
create trigger delete_diagnostics
after delete on taskspecificUpdate
for each row execute procedure do_del_diag_taskspecificUpdate()
;


create table TaskPackageRecordStructure (
targetReference numeric constraint ref_taskpkgrecstructure references taskpackage(targetReference) on delete cascade,
lbnr numeric not null,
constraint pk_TaskPackageRecordStructure primary key (TargetReference, lbnr),
recordOrSurDiag1 varchar(20), --  burde v�re en clob
recordOrSurDiag2 numeric, --  burde references diagnostics, men den er optional
correlationInfo numeric,
recordStatus numeric(1) default 2 not null, --  1=success 2=queued 3=inProcess 4=failure
constraint tprs_recstatus check (recordStatus in (1,2,3,4)),
record_id varchar(200)) -- 001*a from record if valid iso2709,  PID from FEDORA 
;

create or replace function do_upd_taskrecstruc() returns trigger 
LANGUAGE plpgsql
AS
$BODY$
begin
  if new.recordStatus = 1 or new.recordStatus = 4 
then
		update taskspecificUpdate
		set noofrecs_treated=noofrecs_treated+1
		where TargetReference=new.TargetReference;
	end if;  
  return new;
end;
$BODY$
;

DROP TRIGGER IF EXISTS upd_taskrecstruc ON TaskPackageRecordStructure;
create trigger del_diag_taskpackage
after update of recordStatus on TaskPackageRecordStructure
for each row execute procedure do_upd_taskrecstruc()
;


create or replace function do_del_diag_taskrecstruc() returns trigger 
LANGUAGE plpgsql
AS
$BODY$
begin
	delete from diagnostics
	where id=old.recordOrSurDiag2;
  return old;
end;
$BODY$
;


DROP TRIGGER IF EXISTS delete_diagnostics on TaskPackageRecordStructure;
create trigger delete_diagnostics
after delete on TaskPackageRecordStructure
for each row execute procedure do_del_diag_taskrecstruc();


/*
create sequence correlationInfoSeq;

create table correlationInfo (
id numeric not null, --  l�benr fra correlationInfoSeq
lbnr numeric not null,
constraint pk_correlationInfo primary key (id, lbnr),
correlationInfoId numeric,
correlationInfoNote varchar(2000))
;
*/


create table suppliedRecords (
targetReference numeric constraint ref_suppliedRecords references taskspecificUpdate(targetReference) on delete cascade,
lbnr numeric not null,
constraint pk_suppliedRecords primary key (TargetReference, lbnr),
recordId1 numeric,
recordId2 varchar(200),
recordId3 varchar(200),       -- er en blob i z3950 standarden ( OctetString )
supplementalId1 date,
supplementalId2 varchar(200),
supplementalId3 varchar(264), --  er en blob i z3950 standarden ( External ) 
correlationInfo numeric,
record bytea not null,
originalRecord bytea)
;


create or replace function do_ins_update_noofrecs() returns trigger 
LANGUAGE plpgsql
AS
$BODY$
begin  
  update taskspecificUpdate
	set noofrecs=noofrecs+1
	where targetReference=new.targetReference;
	insert into TaskPackageRecordStructure
	(targetReference, lbnr, correlationInfo)
	values (new.targetReference, new.lbnr, new.correlationInfo);
  return new;
end;
$BODY$
;

DROP TRIGGER IF EXISTS ins_update_noofrecs on suppliedRecords;
create trigger ins_update_noofrecs 
after insert on suppliedRecords
for each row execute procedure do_ins_update_noofrecs();


/*
create table taskspecificPersResSet (
targetReference numeric constraint ref_taskspecPersResSet references taskpackage(targetReference) on delete cascade,
constraint pk_taskspecificPersResSet primary key (TargetReference),
setreference varchar(200))
;
create table PersResSet2delete (
creationDate date not null constraint pk_PersResSet2delete primary key,
file2delete varchar(200))
;
create or replace trigger ins_PersResSet2bdeleted
after delete or update on taskspecificPersResSet
for each row
begin
	insert into PersResSet2delete (creationDate, file2delete)
	values (sysdate, :old.setreference);
end;
/
create table taskspecificPersQuery (
targetReference numeric constraint ref_taskspecPersQuery references taskpackage(targetReference) on delete cascade,
constraint pk_taskspecificPersQuery primary key (TargetReference),
additionalSearchInfo numeric,
package varchar(2000),
query_type numeric(3),
query_encoded blob)
;
create sequence specification_seq;

create table specification (
specnr numeric constraint pk_specification primary key, --  fra specification_seq
schema varchar(200),
elementSpec varchar(2000),
externalSpec varchar(2000))
;
create table dbSpecific (
nr numeric constraint pk_dbSpecific primary key,
lbnr numeric not null,
db varchar(2000) not null,
spec numeric constraint ref_dbSpecific references specification(specnr))
;
create table taskspecificExportSpec (
targetReference numeric constraint ref_taskspecExportSpec references taskpackage(targetReference) on delete cascade,
constraint pk_taskspecificExportSpec primary key (targetReference),
select_alt_syntax numeric not null, --  fra compspec boolean 1=true 0=false
constraint tpexportspec_altsyntax check (select_alt_syntax in (0,1)),
generic numeric,                    --  burde references specification(specnr)
dbSpecificnr numeric,               --  references dbSpecific(nr)
recordSyntax varchar(2000),        --  slut compspec
destination_type numeric not null, -- 1=phonenumeric, 2=faxNumeric, 3=x400address, 4=emailAddress, 5=pagerNumeric, 6=ftpAddress, 7=ftamAddress, 8=printerAddress, 100=other vehicle is not null
constraint tpexportspec_dest_type check (destination_type in (1,2,3,4,5,6,7,8,100)),
vehicle varchar(2000),
destination varchar(2000))
;
create sequence searchset_seq;
create table searchset (
nr numeric not null, --  fra searchset_seq
lbnr numeric not null,
constraint pk_searchset primary key (nr, lbnr),
DatabaseName constraint ref_searchset_databases references Databases(DatabaseName),
id numeric,
data blob) --  hvis fremmed db hentes posten og lagres her
;
create table taskspecificExportInv (
targetReference numeric constraint ref_taskspecExportInv references taskpackage(targetReference) on delete cascade,
constraint pk_taskspecificExportInv primary key (targetReference),
packageName varchar(200), --  hvis der refereres til en Export Specification
searchset numeric not null, --  references searchset nr - her lagres de poster/id-nummer+databasename, der skal eksporteres
numericOfCopies numeric,
estimatedQuantity numeric,
quantitySoFar numeric,
estimatedCost numeric,
costSoFar numeric)
;
create or replace trigger exportinv_del
after delete on taskspecificExportInv
for each row
begin
	delete from searchset where nr=:old.searchset;
end;

create table taskspecificPersQSched (
targetReference numeric constraint ref_taskspecPersQSched references taskpackage(targetReference) on delete cascade,
constraint pk_taskspecificPersQSched primary key (targetReference),
activeflag numeric not null, --  boolean
DatabaseName constraint ref_tppersQsched_databases references Databases(DatabaseName),
ResultSetDisp numeric(1) not null, -- 1=replace, 2=append, 3=createNew
constraint tppqsched_ressetdisp check (ResultSetDisp in (1,2,3)),
AlertDestinationType numeric not null, -- 1=phonenumeric, 2=faxNumeric, 3=x400address, 4=emailAddress, 5=pagerNumeric, 6=ftpAddress, 7=ftamAddress, 8=printerAddress, 100=other vehicle is not null
constraint tppqsched_adesttype check (AlertDestinationType in (1,2,3,4,5,6,7,8,100)),
alertVehicle varchar(2000),
AlertDestination varchar(2000),
ExportSpecPkgName varchar(2000),
QyerySpecPkgName varchar(2000),
Query blob,
Period numeric, -- sekunder?
expiration numeric, --  ?
resultsetPkgName varchar(200),
lastQueryTime date,
lastResultNumeric numeric, --  ?
numericSinceModify numeric --  ?
)
;
*/

create table taskspecificItemOrder (
targetReference numeric constraint ref_taskspecItemOrder references taskpackage(targetReference) on delete cascade,
constraint pk_taskspecificItemOrder primary key (targetReference),
supplDescription varchar(200),
contact_name varchar(200),
contact_phone varchar(200),
contact_email varchar(200),
addlBilling numeric(1), -- 1=billInvoice 2=customerReference 3=customerPONumeric
constraint tpio_addlbill check (addlBilling in (1,2,3)),
billInvoice numeric(1), -- 1=prepay 2=depositAccount 3=creditCard 4=cardInfoPrefiouslySupplied, 5=privateKnown 6=privateNotKnown
constraint tpio_bllinvoi check (billInvoice in (1,2,3,4,5,6)),
customerReference  varchar(200),
customerPonumeric varchar(200),
Credit_nameOnCard varchar(200),
Credit_expirationDate varchar(200),
Credit_cardNumeric varchar(200),
searchset numeric, --  kun de items, der skal orderes gemmes i searchset
itemRequest bytea, -- ISO 10161 ILL som en "klump"
statusOrErrorReport varchar(2000), --  ISO 10161 ILL Status-Or-Error-Report
auxiliaryStatus numeric(1), -- 1=notReceived 2=loanQueue 3=forwarded 4=unfilledCopyright 5=filledCopyright
constraint tpio_auxstatus check (auxiliaryStatus in (1,2,3,4,5))
)
;

/*
create or replace function do_itemorder_del() returns trigger 
LANGUAGE plpgsql
AS
$BODY$
begin  
	delete from searchset where nr=old.searchset;
  return old;
end;
$BODY$
;

DROP TRIGGER IF EXISTS itemorder_del ON taskspecificItemOrder;
create trigger itemorder_del 
after delete on taskspecificItemOrder
for each row execute procedure do_itemorder_del ();
*/

create table illsearchrequestkeys (
targetReference numeric constraint ref_illkeys references taskpackage(targetReference) on delete cascade,
constraint pk_illkeys primary key (targetReference),
  transactiongroupqualifier varchar(256) not null,
  transactionqualifier varchar(256) not null,
  subtransactionqualifier varchar(256) not null,
  initialrequesterid varchar(6) not null
)
;

create table trefwait4 (
targetReference numeric constraint tref_depending
references taskpackage(targetReference) on delete cascade,
wait4targetreference numeric constraint tref_dependant
references taskpackage(targetReference) on delete cascade,
constraint pk_trefdependson primary key (targetReference, wait4targetreference)
)
;

create or replace function tp_not_blocked_by_other( tref numeric) returns boolean
  language plpgsql
  as
$BODY$
DECLARE
ant_blocking numeric;
begin
  select count(*) into ant_blocking from trefwait4 w4, taskpackage tp where w4.targetreference=tref and w4.wait4targetreference=tp.targetreference and tp.taskstatus<>2;
  return ant_blocking = 0;
end;
$BODY$
;

create view updatepackages as
SELECT tp.userid, us.update_priority, tp.targetReference, up.databaseName, up.schema, up.elementSetName, up.action, tp.creationdate, tp.accessdate, tp.taskstatus, tp.substatus
FROM Databases db, userids us, taskpackage tp, taskspecificUpdate up
WHERE tp.TargetReference=up.TargetReference
AND tp.taskstatus<2
AND us.userid=tp.userid
AND tp.packageType=5
AND up.Updatestatus=0
AND us.update_priority is not null
AND db.DatabaseName=up.DatabaseName
AND us.update_priority < db.max_update_priority
AND tp_not_blocked_by_other( tp.targetReference )
