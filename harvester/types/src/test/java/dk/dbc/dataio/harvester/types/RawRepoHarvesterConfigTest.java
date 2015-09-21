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

package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RawRepoHarvesterConfigTest {
    @Test
    public void addEntry_entryArgIsValid_addsEntryAndReturnsTrue() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder().build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        assertThat("addEntry() return value", config.addEntry(configEntry), is(true));
        assertThat("entries size", config.getEntries().size(), is(1));
        assertThat("entries contains entry", config.getEntries().contains(configEntry), is(true));
    }

    @Test
    public void addEntry_entryArgIsDuplicate_returnsFalse() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder().build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        config.addEntry(configEntry);
        assertThat("addEntry() return value", config.addEntry(configEntry), is(false));
    }

    @Test
    public void addEntry_entryArgHasNullId_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setId(null)
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasEmptyId_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setId("   ")
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasNullResource_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setResource(null)
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasEmptyResource_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setResource("   ")
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasNullConsumerId_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setConsumerId(null)
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasEmptyConsumerId_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setConsumerId("   ")
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasNullDestination_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setDestination(null)
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasEmptyDestination_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setDestination("   ")
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasNullFormat_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setFormat(null)
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void addEntry_entryArgHasEmptyFormat_throws() {
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder()
                .setFormat("   ")
                .build();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        try {
            config.addEntry(configEntry);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void emptyConfigCanBeMarshalledAndUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        final String marshalled = jsonbContext.marshall(config);
        jsonbContext.unmarshall(marshalled, RawRepoHarvesterConfig.class);
    }

    @Test
    public void nonEmptyConfigCanBeMarshalledAndUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder().build();
        configEntry.setFormatOverride(42, "format42");
        configEntry.setType(JobSpecification.Type.TEST);
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        config.addEntry(configEntry);
        final String marshalled = jsonbContext.marshall(config);
        final RawRepoHarvesterConfig unmarshalled = jsonbContext.unmarshall(marshalled, RawRepoHarvesterConfig.class);
        assertThat("entries", unmarshalled.getEntries().contains(configEntry), is(true));
        for (RawRepoHarvesterConfig.Entry entry : unmarshalled.getEntries()) {
            assertThat("openAgencyTarget", entry.getOpenAgencyTarget(), is(nullValue()));
            assertThat("type", entry.getType(), is(JobSpecification.Type.TEST));
        }
    }

    @Test
    public void nonEmptyConfigWithOpenAgencyTargetCanBeMarshalledAndUnmarshalled() throws JSONBException, MalformedURLException {
        final JSONBContext jsonbContext = new JSONBContext();

        final OpenAgencyTarget openAgencyTarget = new OpenAgencyTarget();
        openAgencyTarget.setUrl(new URL("http://test.dbc.dk/oa"));
        openAgencyTarget.setGroup("groupId");
        openAgencyTarget.setUser("userId");
        openAgencyTarget.setPassword("passw0rd");

        final RawRepoHarvesterConfig.Entry configEntry = new RawRepoHarvesterConfigEntryBuilder().build();
        configEntry.setFormatOverride(42, "format42");
        configEntry.setOpenAgencyTarget(openAgencyTarget);

        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        config.addEntry(configEntry);

        final String marshalled = jsonbContext.marshall(config);
        final RawRepoHarvesterConfig unmarshalled = jsonbContext.unmarshall(marshalled, RawRepoHarvesterConfig.class);
        assertThat("entries", unmarshalled.getEntries().contains(configEntry), is(true));
        for (RawRepoHarvesterConfig.Entry entry : unmarshalled.getEntries()) {
            assertThat("openAgencyTarget", entry.getOpenAgencyTarget(), is(openAgencyTarget));
            assertThat("type", entry.getType(), is(JobSpecification.Type.TRANSIENT));
        }
    }

    @Test
    public void formatCanBeOverridden() {
        final String format = "format";
        final String formatFor42 = "format42";
        final int agencyId42 = 42;
        final RawRepoHarvesterConfig.Entry entry = new RawRepoHarvesterConfigEntryBuilder()
                .setFormat(format)
                .build();
        entry.setFormatOverride(agencyId42, formatFor42);
        assertThat("format", entry.getFormat(), is(format));
        assertThat("format not overridden", entry.getFormat(123), is(format));
        assertThat("format overridden", entry.getFormat(agencyId42), is(formatFor42));
    }

    public static class RawRepoHarvesterConfigEntryBuilder {
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

}