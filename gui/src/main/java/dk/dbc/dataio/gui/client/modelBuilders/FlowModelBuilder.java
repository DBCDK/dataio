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

package dk.dbc.dataio.gui.client.modelBuilders;

import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowModelBuilder {
    private long id = 3L;
    private long version = 1L;
    private String name = "name";
    private String description = "description";
    private String timeOfFlowComponentUpdate = "2016-11-18 15:24:40";
    private List<FlowComponentModel> flowComponents = new ArrayList<FlowComponentModel>(Collections.singletonList(
            new FlowComponentModelBuilder().build()));

    public FlowModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public FlowModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public FlowModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowModelBuilder setTimeOfFlowComponentUpdate(String timeOfFlowComponentUpdate) {
        this.timeOfFlowComponentUpdate = timeOfFlowComponentUpdate;
        return this;
    }

    public FlowModelBuilder setComponents(List<FlowComponentModel> flowComponents) {
        this.flowComponents = new ArrayList<>(flowComponents);
        return this;
    }

    public FlowModel build() {
        return new FlowModel(id, version, name, description, timeOfFlowComponentUpdate, flowComponents);
    }
}
