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

package dk.dbc.dataio.sink.worldcat;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class WorldCatAttributesTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void canBeMarshalledAndUnmarshalled() throws JSONBException {
        final WorldCatAttributes worldCatAttributes = new WorldCatAttributes()
                .withPid("testPid")
                .withOcn("testOcn")
                .withHoldings(Arrays.asList(
                        new Holding().withSymbol("ABCDE").withAction(Holding.Action.INSERT),
                        new Holding().withSymbol("FGHIJ").withAction(Holding.Action.DELETE)
                ));

        final WorldCatAttributes unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(worldCatAttributes), WorldCatAttributes.class);
        assertThat(unmarshalled, is(worldCatAttributes));
    }
}