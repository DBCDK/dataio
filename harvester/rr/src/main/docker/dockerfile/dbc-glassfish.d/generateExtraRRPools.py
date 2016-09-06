import sys, getopt
import json


def getExtraRRConfig( data, filename ):
   try:
    js = json.load(data)

    pools= '''<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE resources PUBLIC "-//GlassFish.org//DTD GlassFish Application Server 3.1 Resource Definitions//EN" "http://glassfish.org/dtds/glassfish-resources_1_5.dtd">
<resources>'''

    for rawrepo in js['rawrepos']:
        pools=pools+'''<jdbc-connection-pool
                          datasource-classname="org.postgresql.ds.PGSimpleDataSource"
                          name="jdbc/dataio/''' + rawrepo['name']+'''/pool">
      <property name="user" value="'''+rawrepo['user']+'''"></property>
      <property name="PortNumber" value="5432"></property>
      <property name="password" value="'''+rawrepo['password']+'''"></property>
      <property name="DriverClass" value="org.postgresql.Driver"></property>
      <property name="DatabaseName" value="'''+rawrepo['db']+'''"></property>
      <property name="serverName" value="'''+rawrepo['server']+'''"></property>
        </jdbc-connection-pool>

        <jdbc-resource pool-name="jdbc/dataio/'''+rawrepo['name']+'''/pool" jndi-name="jdbc/dataio/'''+rawrepo['name']+'''"></jdbc-resource>
    '''
    pools=pools+'</resources>'
    f=open(filename, 'w')
    f.write(str(pools))
    f.close()
   except Exception as e:
       f=open(filename+'-err', 'w')
       f.write(str(e))
       f.close()



def main(argv):
   getExtraRRConfig(data=sys.stdin, filename=argv[0])



if __name__ == "__main__":
    main(sys.argv[1:])


