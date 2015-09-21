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

package dk.dbc.dataio.jobstore.test.types;

import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;

import java.util.HashMap;
import java.util.Map;

public class FlowStoreReferencesBuilder {

    private final Map<FlowStoreReferences.Elements, FlowStoreReference> references = createReferences();

    public FlowStoreReferencesBuilder setFlowStoreReference(FlowStoreReferences.Elements element, FlowStoreReference flowStoreReference) {
        this.references.put(element, flowStoreReference);
        return this;
    }

    public FlowStoreReferences build() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER, references.get(FlowStoreReferences.Elements.FLOW_BINDER));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, references.get(FlowStoreReferences.Elements.FLOW));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK, references.get(FlowStoreReferences.Elements.SINK));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SUBMITTER, references.get(FlowStoreReferences.Elements.SUBMITTER));
        return flowStoreReferences;
    }

    private Map<FlowStoreReferences.Elements, FlowStoreReference> createReferences() {
        Map<FlowStoreReferences.Elements, FlowStoreReference> flowStoreReferences = new HashMap<>(4);
        flowStoreReferences.put(FlowStoreReferences.Elements.FLOW_BINDER, new FlowStoreReferenceBuilder().setName("FlowBinderName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.FLOW, new FlowStoreReferenceBuilder().setName("FlowName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.SINK, new FlowStoreReferenceBuilder().setName("SinkName").build());
        flowStoreReferences.put(FlowStoreReferences.Elements.SUBMITTER, new FlowStoreReferenceBuilder().setName("SubmitterName").build());
        return flowStoreReferences;
    }
}

