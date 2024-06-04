Rere accessnew
===
Cronjob for rerunning records with collection identifier 870970-accessnew, by queryíng corepo-searcher and submitting the records to the appropriate rr harvester 

Usage (Staging):
java -jar dataio-rerun-accessnew-2.0-SNAPSHOT.jar STAGING_CISTERNE

Usage (Prod):
java -jar dataio-rerun-accessnew-2.0-SNAPSHOT.jar <BOBLEBAD|FBSTEST|CISTERNE>+

Example
java -jar dataio-rerun-accessnew-2.0-SNAPSHOT.jar BOBLEBAD FBSTEST CISTERNE

Will rerun all prod environments
