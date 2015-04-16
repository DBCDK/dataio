#!/usr/bin/env bash

function die() {                       echo ERROR: $*
   exit 1
}

#
#
# arguments
GLASSFISHSERVERPORT=""
POSTGRESPORT=
ASADMIN=`which asadmin`
OUTPUT_FILE=./localhost-pg-resorces-2.xml

function help() {
   echo "$0 -f xmlfile -p postgresql -g glassfish-url"
   echo "examples"
   echo "-g http://localhost:8040"
   echo "-a asadmin binary"
}

while getopts "hf:a:g:" opt; do
    case "$opt" in
        h)

        ;;
        g)
           GLASSFISHSERVERPORT="$OPTARG"
        ;;
        a)
           ASADMIN="$OPTARG"
        ;;
        *)
            die "unknown arguments"
        ;;
     esac
done


echo "args"
echo "asadmin=$ASADMIN"
echo "glassfish=$GLASSFISHSERVERPORT"


function start_file() {
   cat - > $OUTPUT_FILE <<EOF
<?xml version="1.0"?>
<!DOCTYPE resources PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Resource Definitions//EN" "http://glassfish.org/dtds/glassfish-resources_1_5.dtd">
   <resources>

EOF
}

function end_file() {
   cat - >> $OUTPUT_FILE <<EOF

   </resources>
EOF
}


# appennd a database block
#
function append_database( ) {
 databaseName=$1
 jndiName=$2

 cat - >> $OUTPUT_FILE <<EOF
    <jdbc-connection-pool datasource-classname="org.postgresql.ds.PGSimpleDataSource" name="${jndiName}/pool" res-type="javax.sql.DataSource">
          <property name="driverClass" value="org.postgresql.Driver"></property>
          <property name="DatabaseName" value="dataio-${databaseName}"></property>
          <property name="BinaryTransfer" value="true"></property>
          <property name="Ssl" value="false"></property>
          <property name="ServerName" value="localhost"></property>
          <property name="ProtocolVersion" value="0"></property>
          <property name="TcpKeepAlive" value="false"></property>
          <property name="SocketTimeout" value="0"></property>
          <property name="PortNumber" value="5432"></property>
          <property name="LoginTimeout" value="0"></property>
          <property name="UnknownLength" value="2147483647"></property>
          <property name="PrepareThreshold" value="5"></property>
          <property name="User" value="dataio"></property>
          <property name="Password" value="dataio"></property>
    </jdbc-connection-pool>
    <jdbc-resource pool-name="${jndiName}/pool" jndi-name="${jndiName}"></jdbc-resource>
EOF
}

function checkArguments() {
 echo .
}


function main() {
checkArguments

start_file
append_database filestore jdbc/dataio/fileStore
append_database flowstore jdbc/flowStoreDb
append_database logstore jdbc/dataio/logstore
append_database jobstore jdbc/dataio/jobstore
end_file


echo $OUTPUT_FILE generated
echo $ASADMIN

}


main
