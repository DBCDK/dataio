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

package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowContentBuilder {
    private String name = "name";
    private String description = "description";
    private List<FlowComponent> components = new ArrayList<>(Collections.singletonList(
            new FlowComponentBuilder().build()));

    public FlowContentBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public FlowContentBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public FlowContentBuilder setComponents(List<FlowComponent> components) {
        this.components = new ArrayList<>(components);
        return this;
    }

    public FlowContent build() {
        return new FlowContent(name, description, components);
    }
}
