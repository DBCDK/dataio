/*
 * DataIO - Data IO
 *
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HarvestSelectorRequestTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void marshalling_unmarshalling() throws JSONBException {
        final HarvestSelectorRequest request = new HarvestSelectorRequest("dataset = 42");
        final String marshalled = jsonbContext.marshall(request);
        final HarvestSelectorRequest unmarshalled = jsonbContext.unmarshall(marshalled, HarvestSelectorRequest.class);
        assertThat(unmarshalled.getSelector(), is(new HarvestTaskSelector("dataset", "42")));
    }
}
