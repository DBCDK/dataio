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

import dk.dbc.dataio.commons.types.Constants;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class UshSolrHarvesterConfigTest {
    private static final long ID = 42L;
    private static final long VERSION = 1L;
    private static final UshSolrHarvesterConfig.Content CONTENT = new UshSolrHarvesterConfig.Content();

    @Test(expected = NullPointerException.class)
    public void constructor_contentArgIsNull_throws() {
        new UshSolrHarvesterConfig(ID, VERSION, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_idArgIsLessThanLowerBound_throws() {
        new UshSolrHarvesterConfig(Constants.PERSISTENCE_ID_LOWER_BOUND - 1, VERSION, CONTENT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_versionArgIsLessThanLowerBound_throws() {
        new UshSolrHarvesterConfig(ID, Constants.PERSISTENCE_VERSION_LOWER_BOUND - 1, CONTENT);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        assertThat(new UshSolrHarvesterConfig(ID, VERSION, CONTENT), is(notNullValue()));
    }

    @Test
    public void content_getTimeOfLastHarvest_dateCanBeNull() {
        final UshSolrHarvesterConfig.Content content = new UshSolrHarvesterConfig.Content();
        assertThat(content.getTimeOfLastHarvest(), is(nullValue()));
    }

    @Test
    public void content_withTimeOfLastHarvest_dateCanBeNull() {
        final UshSolrHarvesterConfig.Content content = new UshSolrHarvesterConfig.Content();
        content.withTimeOfLastHarvest(null);
    }

    @Test
    public void canBeMarshalledAndUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final UshSolrHarvesterConfig config = new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()
                .withName("testConfig"));

        final String marshalled = jsonbContext.marshall(config);
        final UshSolrHarvesterConfig unmarshalled = jsonbContext.unmarshall(marshalled, UshSolrHarvesterConfig.class);
        assertThat(unmarshalled, is(config));
    }

}