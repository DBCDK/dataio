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

package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Assert;
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;

public class ImsSinkConfigTest {

    private static final String ENDPOINT = "endpoint";

    @Test
    public void constructor_endpointArgIsNull_throws() {
        assertThat(() -> new ImsSinkConfig().withEndpoint(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstanceWithDefaultValuesSet() {
        final ImsSinkConfig sinkConfig = new ImsSinkConfig().withEndpoint(ENDPOINT);
        Assert.assertThat(sinkConfig.getEndpoint(), is(ENDPOINT));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final ImsSinkConfig sinkConfig = new ImsSinkConfig();
        final ImsSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(sinkConfig), ImsSinkConfig.class);
        Assert.assertThat(unmarshalled, is(sinkConfig));
    }
}
