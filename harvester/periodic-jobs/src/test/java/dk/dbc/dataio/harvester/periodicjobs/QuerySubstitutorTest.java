/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class QuerySubstitutorTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setTimeZone() {
        environmentVariables.set("TZ", "Europe/Copenhagen");
    }

    @Test
    public void replace_timeOfLastHarvest() {
        final ZonedDateTime timeOfLastHarvest = Instant.parse("2019-01-14T07:00:00.00Z")
                .atZone(ZoneId.of(System.getenv("TZ")));
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content()
                        .withTimeOfLastHarvest(Date.from(timeOfLastHarvest.toInstant())));
        final QuerySubstitutor querySubstitutor = new QuerySubstitutor();
        assertThat(querySubstitutor.replace("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]", config),
                is("datefield:[2019-01-14T07:00Z TO *]"));
    }
    
    @Test
    public void replace_timeOfLastHarvest_whenNull() {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content());
        final QuerySubstitutor querySubstitutor = new QuerySubstitutor();
        assertThat(querySubstitutor.replace("datefield:[${__TIME_OF_LAST_HARVEST__} TO *]", config),
                is("datefield:[1970-01-01T00:00Z TO *]"));
    }

    @Test
    public void replace_now() {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content());
        final QuerySubstitutor querySubstitutor = new QuerySubstitutor();
        final String query = querySubstitutor.replace(
                "datefield:[${__TIME_OF_LAST_HARVEST__} TO ${__NOW__}]", config);
        assertThat("__TIME_OF_LAST_HARVEST__",
                query, containsString("1970-01-01T00:00Z"));
        assertThat("__NOW__",
                query, not(containsString("__NOW__")));
    }

    @Test
    public void replace_nonMatching() {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 2,
                new PeriodicJobsHarvesterConfig.Content());
        final QuerySubstitutor querySubstitutor = new QuerySubstitutor();
        assertThat(querySubstitutor.replace("datefield:[__TIME_OF_LAST_HARVEST__ TO ${__THEN__}]", config),
                is("datefield:[__TIME_OF_LAST_HARVEST__ TO ${__THEN__}]"));
    }
}