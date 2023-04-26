package dk.dbc.dataio.commons.types.jms;

/**
 * Constant values used be the dataIO JMS messaging protocol.
 * Deprecated: Please use JMSHeaders instead
 */
@Deprecated
public class JmsConstants {
    public static final String PAYLOAD_PROPERTY_NAME = "payload";
    public static final String RESOURCE_PROPERTY_NAME = "resource";
    public static final String SINK_ID_PROPERTY_NAME = "id";
    public static final String SINK_VERSION_PROPERTY_NAME = "version";
    public static final String FLOW_ID_PROPERTY_NAME = "flowId";
    public static final String FLOW_VERSION_PROPERTY_NAME = "flowVersion";
    public static final String FLOW_BINDER_ID_PROPERTY_NAME = "flowBinderId";
    public static final String FLOW_BINDER_VERSION_PROPERTY_NAME = "flowBinderVersion";
    public static final String PROCESSOR_SHARD_PROPERTY_NAME = "shard";

    public static final String CHUNK_PAYLOAD_TYPE = "Chunk";
    public static final String JOB_STORE_SOURCE_VALUE = "jobstore";

    public static final String ADDITIONAL_ARGS = "additionalArgs";

    private JmsConstants() {
    }
}
