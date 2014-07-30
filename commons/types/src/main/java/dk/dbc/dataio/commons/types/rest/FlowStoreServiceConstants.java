package dk.dbc.dataio.commons.types.rest;

public class FlowStoreServiceConstants {
    public static final String FLOWS = "flows";
    public static final String FLOW_BINDERS = "binders";
    public static final String FLOW_COMPONENTS = "components";
    public static final String SUBMITTERS = "submitters";
    public static final String SINKS = "sinks";

    public static final String SINK_ID_VARIABLE = "id";
    public static final String FLOW_ID_VARIABLE = "id";
    public static final String FLOW_COMPONENT_ID_VARIABLE = "id";

    public static final String SINK = "sinks/{id}";
    public static final String FLOW = "flows/{id}";
    public static final String FLOW_COMPONENT = "components/{id}";
    public static final String SINK_CONTENT = "sinks/{id}/content";
    public static final String FLOW_COMPONENT_CONTENT = "components/{id}/content";

    public static final String IF_MATCH_HEADER = "If-Match";

    private FlowStoreServiceConstants() { }
}