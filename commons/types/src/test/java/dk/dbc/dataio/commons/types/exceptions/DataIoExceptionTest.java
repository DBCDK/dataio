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

package dk.dbc.dataio.commons.types.exceptions;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class DataIoExceptionTest {

    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void zeroArgConstructor_setsProperties() {
        final DataIoException dataIoException = new DataIoException();
        assertThat(dataIoException.getType(), is(DataIoException.class.getName()));
        assertThat(dataIoException.getCause(), is(nullValue()));
        assertThat(dataIoException.getMessage(), is(nullValue()));
        assertThat(dataIoException.getCausedBy(), is(nullValue()));
        assertThat(dataIoException.getCausedByDetail(), is(nullValue()));
    }

    @Test
    public void oneArgConstructor_setsProperties() {
        final String message = "message";
        final DataIoException dataIoException = new DataIoException(message);
        assertThat(dataIoException.getType(), is(DataIoException.class.getName()));
        assertThat(dataIoException.getCause(), is(nullValue()));
        assertThat(dataIoException.getMessage(), is(message));
        assertThat(dataIoException.getCausedBy(), is(nullValue()));
        assertThat(dataIoException.getCausedByDetail(), is(nullValue()));
    }

    @Test
    public void twoArgConstructor_setsProperties() {
        final String message = "message";
        final String causedByDetail = "detail";
        final NullPointerException cause = new NullPointerException(causedByDetail);
        final DataIoException dataIoException = new DataIoException(message, cause);
        assertThat(dataIoException.getType(), is(DataIoException.class.getName()));
        assertThat(dataIoException.getCause(), CoreMatchers.<Throwable>is(cause));
        assertThat(dataIoException.getMessage(), is(message));
        assertThat(dataIoException.getCausedBy(), is(cause.getClass().getName()));
        assertThat(dataIoException.getCausedByDetail(), is(causedByDetail));
    }

    @Test
    public void serializationOfSubType_followedBy_deserializationIntoSuperType() throws JSONBException {
        DataIoException dataIoException;
        final String message = "message";
        final String causedByDetail = "detail";
        final NullPointerException cause = new NullPointerException(causedByDetail);
        try {
            throw new TestException(message, cause);
        } catch (DataIoException e) {
            dataIoException = jsonbContext.unmarshall(jsonbContext.marshall(e), DataIoException.class);
        }
        assertThat(dataIoException.getType(), is(TestException.class.getName()));
        assertThat(dataIoException.getMessage(), is(message));
        assertThat(dataIoException.getCausedBy(), is(cause.getClass().getName()));
        assertThat(dataIoException.getCausedByDetail(), is(causedByDetail));
    }

    private static final class TestException extends DataIoException {

        public TestException(String message, Exception cause) {
            super(message, cause);
        }
    }
}
