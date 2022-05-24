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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FlowStoreErrorTest {
    private static final FlowStoreError.Code CODE = FlowStoreError.Code.NONEXISTING_SUBMITTER;
    private static final String DESCRIPTION = "description";
    private static final String STACKTRACE = "stacktrace";

    @Test(expected = NullPointerException.class)
    public void constructor_codeArgIsNull_throws() {
        new FlowStoreError(null, DESCRIPTION, STACKTRACE);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_descriptionArgIsNull_throws() {
        new FlowStoreError(CODE, null, STACKTRACE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_descriptionArgIsEmpty_throws() {
        new FlowStoreError(CODE, "", STACKTRACE);
    }

    @Test
    public void constructor_stacktraceIsNull_returnsNewInstanceWithEmptyStacktrace() {
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, null);
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void constructor_stacktraceIsEmpty_returnsNewInstanceWithEmptyStacktrace() {
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, "");
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getStacktrace().isEmpty(), is(true));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, STACKTRACE);
        assertThat(FlowStoreError, is(notNullValue()));
        assertThat(FlowStoreError.getCode(), is(CODE));
        assertThat(FlowStoreError.getDescription(), is(DESCRIPTION));
        assertThat(FlowStoreError.getStacktrace(), is(STACKTRACE));
    }

    @Test
    public void jsonBinding_marshallingFollowedByUnmarshalling_returnsNewInstanceWithMatchingFields() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final FlowStoreError FlowStoreError = new FlowStoreError(CODE, DESCRIPTION, STACKTRACE);
        final String marshalled = jsonbContext.marshall(FlowStoreError);
        final FlowStoreError unmarshalled = jsonbContext.unmarshall(marshalled, FlowStoreError.class);
        assertThat(unmarshalled, is(notNullValue()));
        assertThat(unmarshalled.getCode(), is(FlowStoreError.getCode()));
        assertThat(unmarshalled.getDescription(), is(FlowStoreError.getDescription()));
        assertThat(unmarshalled.getStacktrace(), is(FlowStoreError.getStacktrace()));
    }

}
