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

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TickleRepoHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaultJsonEncodeDecode() throws Exception {
        final TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2,
                new TickleRepoHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final TickleRepoHarvesterConfig configFromString =
                jsonbContext.unmarshall(configAsString, TickleRepoHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString,
                is(config));
        assertThat("type default", configFromString.getContent().getType(),
                is(JobSpecification.Type.TRANSIENT));
        assertThat("enabled default", configFromString.getContent().isEnabled(),
                is(false));
        assertThat("notificationsEnabled default", configFromString.getContent().hasNotificationsEnabled(),
                is(false));
    }

    @Test
    public void complexEncodeDecode() throws Exception {
        final TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2,
                new TickleRepoHarvesterConfig.Content()
                        .withId("-id-")
                        .withDatasetName("-name")
                        .withDescription("-description-")
                        .withDestination("-destination-")
                        .withFormat("-format-")
                        .withType(JobSpecification.Type.TEST)
                        .withEnabled(true)
                        .withNotificationsEnabled(true)
        );
        final String configAsString = jsonbContext.marshall(config);

        final TickleRepoHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, TickleRepoHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }

    @Test
    public void getHarvesterToken() {
        final TickleRepoHarvesterConfig config = new TickleRepoHarvesterConfig(1, 2, new TickleRepoHarvesterConfig.Content());
        assertThat(config.getHarvesterToken(42), is("tickle-repo:1:2:42"));
        assertThat(config.getHarvesterToken(0), is("tickle-repo:1:2"));
    }
}