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

package dk.dbc.dataio.harvester.test.types;


import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;

public class RawRepoHarvesterConfigEntryBuilder {
    private String id = "id";
    private String resource = "resource";
    private String consumerId = "consumerId";
    private String format = "format";
    private String destination = "destination";
    private JobSpecification.Type type = JobSpecification.Type.TRANSIENT;

    public RawRepoHarvesterConfigEntryBuilder setId(String id) {
        this.id = id;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setConsumerId(String consumerId) {
        this.consumerId = consumerId;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setDestination(String destination) {
        this.destination = destination;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setType(JobSpecification.Type type) {
        this.type = type;
        return this;
    }

    public RawRepoHarvesterConfig.Entry build() {
        return new RawRepoHarvesterConfig.Entry()
                .setId(id)
                .setResource(resource)
                .setConsumerId(consumerId)
                .setFormat(format)
                .setDestination(destination)
                .setType(type);
    }
}
