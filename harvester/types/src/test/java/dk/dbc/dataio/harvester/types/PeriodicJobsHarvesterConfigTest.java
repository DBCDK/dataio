package dk.dbc.dataio.harvester.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat("harvester type default", configFromString.getContent().getHarvesterType(),
                is(PeriodicJobsHarvesterConfig.HarvesterType.STANDARD));
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
                        .withHarvesterType(PeriodicJobsHarvesterConfig.HarvesterType.SUBJECT_PROOFING)
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
