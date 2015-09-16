package dk.dbc.dataio.commons.types.jndi;

public class JndiConstants {
    public static final String JDBC_RESOURCE_JOBSTORE = "jdbc/dataio/jobstore";
    public static final String JDBC_RESOURCE_LOGSTORE = "jdbc/dataio/logstore";
    public static final String JDBC_RESOURCE_ES_INFLIGHT = "jdbc/dataio/sinks/esInFlight";
    public static final String JDBC_RESOURCE_SINK_DIFF = "jdbc/dataio/diff";

    public static final String URL_RESOURCE_FBS_WS = "url/dataio/fbs/ws";
    public static final String URL_RESOURCE_FILESTORE_RS = "url/dataio/filestore/rs";
    public static final String URL_RESOURCE_LOGSTORE_RS = "url/dataio/logstore/rs";
    public static final String URL_RESOURCE_JOBSTORE_RS = "url/dataio/jobstore/rs";

    public static final String CONFIG_RESOURCE_HARVESTER_RR = "config/dataio/harvester/rr";

    public static final String MAIL_RESOURCE_JOBSTORE_NOTIFICATIONS = "mail/dataio/jobstore/notifications";

    public static final String JMS_QUEUE_PROCESSOR = "jms/dataio/processor"; //processorJmsQueue
    public static final String JMS_QUEUE_SINK = "jms/dataio/sinks"; //sinksJmsQueue


    private JndiConstants() { }
}
