/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class InfomediaHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaults() throws JSONBException {
        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final InfomediaHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, InfomediaHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString,
                is(config));
        assertThat("enabled default", configFromString.getContent().isEnabled(),
                is(false));
    }

    @Test
    public void marshalling() throws JSONBException {
        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(1, 2,
                new InfomediaHarvesterConfig.Content()
                        .withId("-id-")
                        .withSchedule("* * * * *")
                        .withSubmitter("123456")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withEnabled(true)
                        .withTimeOfLastHarvest(new Date())
        );
        final String configAsString = jsonbContext.marshall(config);

        final InfomediaHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, InfomediaHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }
}