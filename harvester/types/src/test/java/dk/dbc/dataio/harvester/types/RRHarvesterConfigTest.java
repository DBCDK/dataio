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
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RRHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaultJsonEncodeDecode() throws Exception {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 2, new RRHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final RRHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, RRHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString, is(config));
        assertThat("includeRelations default", configFromString.getContent().hasIncludeRelations(), is(true));
        assertThat("includeLibraryRules default", configFromString.getContent().hasIncludeLibraryRules(), is(false));
        assertThat("imsHarvester default", configFromString.getContent().getHarvesterType(), is(RRHarvesterConfig.HarvesterType.STANDARD));
    }

    @Test
    public void complexEncodeDecode() throws Exception {
        final RRHarvesterConfig config = new RRHarvesterConfig(1, 2,
                new RRHarvesterConfig.Content()
                        .withId("harvest log id")
                        .withDescription("Description")
                        .withEnabled(true)
                        .withResource("Resource")
                        .withConsumerId("ConsumerId")
                        .withDestination("Destination")
                        .withType(JobSpecification.Type.ACCTEST)
                        .withFormat("format")
                        .withFormatOverridesEntry(12, "formatX")
                        .withFormatOverridesEntry(191919, "formatY")
                        .withIncludeRelations(false)
                        .withIncludeLibraryRules(true)
                        .withBatchSize(12)
                        .withImsHoldingsTarget("ImsHoldingsTarget")
                        .withNote("Note")
        );

        final String configAsString = jsonbContext.marshall(config);

        final RRHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, RRHarvesterConfig.class);
        assertThat("unmarshalling", configFromString, is(config));
        assertThat("expand default", config.getContent().expand(), is(true));
    }

    @Test
    public void getHarvesterToken() {
        final RRHarvesterConfig config = new RRHarvesterConfig(42, 1, new RRHarvesterConfig.Content());
        assertThat(config.getHarvesterToken(), is("raw-repo:42:1"));
    }
}
