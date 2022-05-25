package dk.dbc.dataio.jobstore.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.invariant.InvariantUtil;

/**
 * This class contains specialized trimming functionality for flows.
 * To optimize caching behaviour the job-store will sometimes want to
 * remove next components from flows prior to caching.
 */
public class FlowTrimmer {
    private final JSONBContext jsonbContext;

    /**
     * @param jsonbContext JSON binding context used to create internal JSON representations
     * @throws NullPointerException if given null-valued jsonbContext
     */
    public FlowTrimmer(JSONBContext jsonbContext) throws NullPointerException {
        this.jsonbContext = InvariantUtil.checkNotNullOrThrow(jsonbContext, "jsonbContext");
    }

    /**
     * Trims next components from given Flow JSON document
     * @param flowJson JSON document representing flow to be trimmed
     * @return trimmed flow as JSON document
     * @throws JSONBException if unable to unmarshall JSON document into internal representation
     */
    public String trim(String flowJson) throws JSONBException {
        final JsonNode flowNode = jsonbContext.getJsonTree(flowJson);
        if (!flowNode.isObject()) {
            throw new IllegalArgumentException("Node is not of type OBJECT but was " + flowNode.getNodeType());
        }
        final JsonNode contentNode = flowNode.path("content");
        final JsonNode componentsNode = contentNode.path("components");
        if (componentsNode.isArray()) {
            for (final JsonNode componentNode : componentsNode) {
                trimFlowComponent(componentNode);
            }
        }
        return flowNode.toString();
    }

    private void trimFlowComponent(JsonNode componentNode) throws IllegalArgumentException {
        if (!componentNode.isObject()) {
            throw new IllegalArgumentException("Node is not of type OBJECT but was " + componentNode.getNodeType());
        }
        ((ObjectNode) componentNode).remove("next");
    }
}
