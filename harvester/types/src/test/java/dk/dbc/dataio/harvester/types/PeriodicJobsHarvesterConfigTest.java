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

public class PeriodicJobsHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaults() throws JSONBException {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final PeriodicJobsHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, PeriodicJobsHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString,
                is(config));
        assertThat("enabled default", configFromString.getContent().isEnabled(),
                is(false));
    }

    @Test
    public void marshalling() throws JSONBException {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("-name-")
                        .withDescription("-description-")
                        .withSchedule("* * * * *")
                        .withQuery("*:*")
                        .withCollection("-collection-")
                        .withTimeOfLastHarvest(new Date())
                        .withResource("-resource-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withSubmitterNumber("-submitter-")
                        .withContact("-contact-")
                        .withEnabled(true)
        );
        final String configAsString = jsonbContext.marshall(config);

        final PeriodicJobsHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, PeriodicJobsHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }

    @Test
    public void getHarvesterToken() {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content());
        assertThat(config.getHarvesterToken(), is("periodic-jobs:1:2"));
    }
}