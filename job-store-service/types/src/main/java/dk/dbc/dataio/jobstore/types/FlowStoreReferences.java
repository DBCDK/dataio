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

package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class FlowStoreReferences {

    @JsonProperty
    private final Map<Elements, FlowStoreReference> references;

    @JsonCreator
    public FlowStoreReferences() {
        this.references = new HashMap<>(Elements.values().length);
    }

    public enum Elements { FLOW_BINDER, FLOW, SUBMITTER, SINK }

    /**
     * Sets the specified reference to a flow store element
     *
     * @param element the flow store element
     * @param flowStoreReference the flow store reference
     */
    public void setReference(FlowStoreReferences.Elements element, FlowStoreReference flowStoreReference) {
        references.put(element, flowStoreReference);
    }

    /**
     * Retrieves the specified flowStoreReference from references
     *
     * @param elements (flow binder, flow, submitter, sink)
     * @return the flow store reference for the specified reference
     */
    public FlowStoreReference getReference(Elements elements) {
        return references.get(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlowStoreReferences)) return false;

        FlowStoreReferences that = (FlowStoreReferences) o;

        if (!references.equals(that.references)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return references.hashCode();
    }
}
