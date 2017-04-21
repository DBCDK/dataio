/*
 *
 *  * DataIO - Data IO
 *  * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 *  * Denmark. CVR: 15149043
 *  *
 *  * This file is part of DataIO.
 *  *
 *  * DataIO is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * DataIO is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package dk.dbc.dataio.harvester.types;

import dk.dbc.dataio.jsonb.JSONBContext;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PhHoldingsItemHarvesterConfigTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void defaultJsonEncodeDecode() throws Exception {
        final PhHoldingsItemsHarvesterConfig config = new PhHoldingsItemsHarvesterConfig(1, 2, new PhHoldingsItemsHarvesterConfig.Content());
        final String configAsString = jsonbContext.marshall(config);

        final PhHoldingsItemsHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, PhHoldingsItemsHarvesterConfig.class);
        assertThat("config unmarshalling", configFromString, is(config));
    }

    @Test
    public void complexEncodeDecode() throws Exception {
        final ArrayList<Long> rrHarvesters = new ArrayList<>(2);
        rrHarvesters.add(654L);
        rrHarvesters.add(321L);
        final PhHoldingsItemsHarvesterConfig config = new PhHoldingsItemsHarvesterConfig(3, 4,
                new PhHoldingsItemsHarvesterConfig.Content()
                        .withName("Namo")
                        .withDescription("Descripo")
                        .withTimeOfLastHarvest(new Date(12345678))
                        .withResource("Resource")
                        .withEnabled(true)
                        .withRrHarvesters(rrHarvesters)
        );
        final String configAsString = jsonbContext.marshall(config);

        final PhHoldingsItemsHarvesterConfig configFromString = jsonbContext.unmarshall(configAsString, PhHoldingsItemsHarvesterConfig.class);
        assertThat(configFromString, is(config));
    }
}