# Gatekeeper
The dataIO Gatekeeper daemon works by monitoring a specified directory for file
system changes.

When a transfile (*.trans) is determined to be complete (i.e. it contains the
'slut' end marker) the number of required modifications (file system changes,
dataIO job creation etc.) are firstly stored in a write-ahead-log kept in
gatekeeper.wal.* files in the daemons current working directory. Subsequently
the modifications are locked and executed sequentially.

In case of shutdown the daemon will try its best to finish any ongoing
operation and leave the system in a consistent state. Although in the case of
an abrupt termination system corruption may occur, and the write-ahead-log
should contain an already locked modification and the daemon will refuse to
start. It is then up to the operations people to resolve these situations
manually.

When all modifications have completed successfully the transfile and its
corresponding data file(s) will no longer be present in the directory and the
write-ahead-log will be empty.

## The java program

Usage for the gatekeeper daemonen:

`java -jar dataio-gatekeeper.jar -h`  
```
usage: java -jar <jarfile>
  -d,--guarded-dir <dir>              Path of directory guarded by this gatekeeper instance               |  
  -f,--file-store-service-url <url>   Base URL of file-store service |
  -j,--job-store-service-url <url>    Base URL of job-store service |  
  -s,--shadow-dir <dir>               Path of shadow directory |  
  -h,--help                           Produce help message   
```
The 'guarded-dir' is the directory monitored for file system changes.

The 'shadow-dir' is where files destined for the old 'posthus' system are
moved to.

The 'file-store-service-url' and 'job-store-service-url' are used to reach
the REST services needed for dataIO job creation.
  
## The local H2 database
To access the write-ahead-log from the console:  
`java -cp dataio-gatekeeper.jar org.h2.tools.Shell`

Welcome to H2 Shell 1.3.171 (2013-03-17)
Exit with Ctrl+C
[Enter]   jdbc:h2:tcp://localhost:9092/mem:test
URL       jdbc:h2:file:///path/to/gatekeeper.wal
[Enter]   org.h2.Driver
Driver
[Enter]   root
User      gatekeeper
[Enter]   Hide
Password  gatekeeper
Connected

sql> show tables;
TABLE_NAME   | TABLE_SCHEMA
MODIFICATION | PUBLIC
SEQUENCE     | PUBLIC
(2 rows, 9 ms)
sql>

## Gatekeeper service
On the servers gatekeeper is started with systemctl, which will also restart it as needed and desired.
The configuration is read from /etc/gatekeeper and the systemd configuration is in /etc/systemd/system/gatekeeper.service

You can start gatekeeper by running:  
`sudo systemctl start gatekeeper.service`

Furthermore stop, restart and status are supported.

### Service configuration
Gate keeper relies on a couple of other DataIO service and an incoming ftp directory these are configured in /etc/gatekeeper

## Deployment
Gatekeeper basically conists of a jar file, a configuration and a service definition. Of these only the jar file is deployed by jenkins,
everything else is maintained manually.


## install.sh
This install script was not actually used to install a server, but more a recollection of what was done, so handle with care.  
