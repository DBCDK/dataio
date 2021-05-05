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
import org.junit.Test;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EsSinkConfigTest {

    private static final Integer USER_ID = 0;
    private static final String DATABASE_NAME = "databaseName";

    @Test
    public void constructor_userIdArgIsNull_throws() {
        assertThat(() -> new EsSinkConfig().withUserId(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_databaseArgIsNull_throws() {
        assertThat(() -> new EsSinkConfig().withDatabaseName(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstanceWithDefaultValuesSet() {
        final EsSinkConfig esSinkConfig = new EsSinkConfig().withUserId(USER_ID).withDatabaseName(DATABASE_NAME);
        assertThat(esSinkConfig.getUserId(), is(USER_ID));
        assertThat(esSinkConfig.getDatabaseName(), is(DATABASE_NAME));
        assertThat(esSinkConfig.getEsAction(), is("INSERT"));
    }

    @Test
    public void marshalling() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final EsSinkConfig esSinkConfig = new EsSinkConfig();
        final EsSinkConfig unmarshalled = jsonbContext.unmarshall(jsonbContext.marshall(esSinkConfig), EsSinkConfig.class);
        assertThat(unmarshalled, is(esSinkConfig));
    }
}
