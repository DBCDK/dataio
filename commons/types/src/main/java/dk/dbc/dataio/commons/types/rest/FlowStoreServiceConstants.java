package dk.dbc.dataio.commons.types.rest;

public class FlowStoreServiceConstants {
    public static final String FLOWS = "flows";
    public static final String FLOW_BINDERS = "binders";
    public static final String FLOW_COMPONENTS = "components";
    public static final String SUBMITTERS = "submitters";
    public static final String SINKS = "sinks";

    public static final String ID_VARIABLE = "id";
    public static final String TYPE_VARIABLE = "type";

    public static final String SINK = "sinks/{id}";
    public static final String SUBMITTER = "submitters/{id}";
    public static final String FLOW = "flows/{id}";
    public static final String FLOW_BINDER = "binders/{id}";
    public static final String FLOW_COMPONENT = "components/{id}";

    public static final String FLOW_BINDERS_QUERIES = "binders/queries";
    public static final String SUBMITTERS_QUERIES = "submitters/queries";

    public static final String SINK_CONTENT = "sinks/{id}/content";
    public static final String SUBMITTER_CONTENT = "submitters/{id}/content";
    public static final String SUBMITTER_FLOW_BINDERS = "submitters/{id}/binders";
    public static final String FLOW_COMPONENT_CONTENT = "components/{id}/content";
    public static final String FLOW_COMPONENT_NEXT = "components/{id}/next";
    public static final String FLOW_CONTENT = "flows/{id}/content";
    public static final String FLOW_BINDER_CONTENT = "binders/{id}/content";

    public static final String QUERY_PARAMETER_REFRESH = "refresh";

    public static final String IF_MATCH_HEADER = "If-Match";
    public static final String RESOURCE_TYPE_HEADER = "Resource-type";

    public static final String FLOW_BINDER_RESOLVE = "binders/resolve";

    public static final String SUBMITTER_SEARCHES_NUMBER = "submitters/searches/number/{number}";
    public static final String SUBMITTER_NUMBER_VARIABLE = "number";

    public static final String HARVESTERS_RR_CONFIG = "harvesters/rr/config";
    public static final String HARVESTER_CONFIGS_TYPE = "harvester-configs/types/{type}";
    public static final String HARVESTER_CONFIGS_TYPE_ENABLED = "harvester-configs/types/{type}/enabled";
    public static final String HARVESTER_CONFIG = "harvester-configs/{id}";

    public static final String GATEKEEPER_DESTINATIONS = "gatekeeper/destinations";
    public static final String GATEKEEPER_DESTINATION = "gatekeeper/destinations/{id}";

    private FlowStoreServiceConstants() {
    }
}
