/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;

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
