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
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;

import java.util.HashMap;
import java.util.Map;

public class RawRepoHarvesterConfigEntryBuilder {
    private String id = "id";
    private String resource = "resource";
    private String consumerId = "consumerId";
    private String destination = "destination";
    private JobSpecification.Type type = JobSpecification.Type.TRANSIENT;
    private String format = "format";
    private final Map<Integer, String> formatOverrides = new HashMap<>();
    private boolean includeRelations = true;
    private int batchSize = 10000;
    private OpenAgencyTarget openAgencyTarget = new OpenAgencyTarget();

    public RawRepoHarvesterConfigEntryBuilder() {
        openAgencyTarget.setUrl("url");
        openAgencyTarget.setGroup("group");
        openAgencyTarget.setUrl("user");
        openAgencyTarget.setPassword("password");
    }


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

    public RawRepoHarvesterConfigEntryBuilder setType(JobSpecification.Type type) {
        this.type = type;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setFormatOverrides(Integer agencyId, String format) {
        this.formatOverrides.put(agencyId, format);
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setIncludeRelations(Boolean includeRelations) {
        this.includeRelations = includeRelations;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public RawRepoHarvesterConfigEntryBuilder setOpenAgencyTarget(OpenAgencyTarget openAgencyTarget) {
        this.openAgencyTarget = openAgencyTarget;
        return this;
    }

    public RawRepoHarvesterConfig.Entry build() {
        RawRepoHarvesterConfig.Entry entry = new RawRepoHarvesterConfig.Entry()
                .setId(id)
                .setResource(resource)
                .setConsumerId(consumerId)
                .setDestination(destination)
                .setType(type)
                .setFormat(format)
                .setIncludeRelations(includeRelations)
                .setBatchSize(batchSize)
                .setOpenAgencyTarget(openAgencyTarget);
        if (!formatOverrides.isEmpty()) {
            for (Map.Entry<Integer, String> formatOverride : formatOverrides.entrySet()) {
                entry.setFormatOverride(formatOverride.getKey(), formatOverride.getValue());
            }
        }
        return entry;
    }

}
