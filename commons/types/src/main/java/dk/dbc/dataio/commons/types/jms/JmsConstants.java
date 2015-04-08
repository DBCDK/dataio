package dk.dbc.dataio.commons.types.jms;

/**
 * Constant values used be the dataIO JMS messaging protocol.
 */
public class JmsConstants {
    public static final String SOURCE_PROPERTY_NAME = "source";
    public static final String PAYLOAD_PROPERTY_NAME = "payload";
    public static final String RESOURCE_PROPERTY_NAME = "resource";

    public static final String CHUNK_PAYLOAD_TYPE = "ExternalChunk";

    public static final String JOB_STORE_SOURCE_VALUE = "jobstore";
    public static final String PROCESSOR_SOURCE_VALUE = "processor";
    public static final String SINK_SOURCE_VALUE = "sink";

    private JmsConstants() { }
}
