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

import dk.dbc.dataio.gui.client.model.SinkModel;

public class SinkModelBuilder {
    private long id = 64L;
    private long version = 1L;
    private String name = "name";
    private String resource = "resource";
    private String description = "description";

    public SinkModelBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public SinkModelBuilder setVersion(long version) {
        this.version = version;
        return this;
    }

    public SinkModelBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public SinkModelBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public SinkModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }


    public SinkModel build() {
        return new SinkModel(id, version, name, resource, description);
    }
}
