package dk.dbc.dataio.commons.types.rest;

public class FlowStoreServiceConstants {
    public static final String FLOWS = "flows";
    public static final String FLOW_BINDERS = "binders";
    public static final String FLOW_COMPONENTS = "components";
    public static final String SUBMITTERS = "submitters";

    public static final String SINKS = "sinks";
    public static final String SINKS_CONTENT_VARIABLE = "content";
    public static final String SINK_ID_VARIABLE = "id";
    public static final String SINK_VERSION_VARIABLE = "version";

    public static final String SINK_ID = "/{id}";
    public static final String SINKS_CONTENT = "/{id}/{version}/content";

    private FlowStoreServiceConstants() { }
}