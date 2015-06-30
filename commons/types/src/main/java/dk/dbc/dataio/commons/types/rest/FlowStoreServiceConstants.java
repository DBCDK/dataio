package dk.dbc.dataio.commons.types.rest;

public class FlowStoreServiceConstants {
    public static final String FLOWS = "flows";
    public static final String FLOW_BINDER = "binders/{id}";
    public static final String FLOW_BINDERS = "binders";
    public static final String FLOW_COMPONENTS = "components";
    public static final String SUBMITTERS = "submitters";
    public static final String SINKS = "sinks";

    public static final String SINK_ID_VARIABLE = "id";
    public static final String FLOW_ID_VARIABLE = "id";
    public static final String SUBMITTER_ID_VARIABLE = "id";
    public static final String FLOW_COMPONENT_ID_VARIABLE = "id";
    public static final String FLOW_BINDER_ID_VARIABLE = "id";

    public static final String SINK = "sinks/{id}";
    public static final String SUBMITTER = "submitters/{id}";
    public static final String FLOW = "flows/{id}";
    public static final String FLOW_COMPONENT = "components/{id}";

    public static final String SINK_CONTENT = "sinks/{id}/content";
    public static final String SUBMITTER_CONTENT = "submitters/{id}/content";
    public static final String SUBMITTER_DELETE = "submitters/{id}/delete";
    public static final String FLOW_COMPONENT_CONTENT = "components/{id}/content";
    public static final String FLOW_CONTENT = "flows/{id}/content";
    public static final String FLOW_BINDER_CONTENT = "binders/{id}/content";

    public static final String QUERY_PARAMETER_REFRESH = "refresh";

    public static final String IF_MATCH_HEADER = "If-Match";

    public static final String FLOW_BINDER_RESOLVE = "binders/resolve";

    public static final String SUBMITTER_SEARCHES_NUMBER = "submitters/searches/number/{number}";
    public static final String SUBMITTER_NUMBER_VARIABLE = "number";

    private FlowStoreServiceConstants() { }
}