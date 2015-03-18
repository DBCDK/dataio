package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
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
        final RawRepoHarvesterConfig config = new RawRepoHarvesterConfig();
        config.addEntry(configEntry);
        final String marshalled = jsonbContext.marshall(config);
        final RawRepoHarvesterConfig unmarshalled = jsonbContext.unmarshall(marshalled, RawRepoHarvesterConfig.class);
        assertThat(unmarshalled.getEntries().contains(configEntry), is(true));
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

        public RawRepoHarvesterConfig.Entry build() {
            return new RawRepoHarvesterConfig.Entry()
                    .setId(id)
                    .setResource(resource)
                    .setConsumerId(consumerId)
                    .setFormat(format)
                    .setDestination(destination);
        }
    }

}