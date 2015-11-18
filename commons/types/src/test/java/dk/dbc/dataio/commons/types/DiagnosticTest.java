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

import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class DiagnosticTest {
    @Test(expected = NullPointerException.class)
    public void constructor2arg_levelArgIsNull_throws() {
        new Diagnostic(null, "message");
    }

    @Test(expected = NullPointerException.class)
    public void constructor2arg_messageArgIsNull_throws() {
        new Diagnostic(Diagnostic.Level.FATAL, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor2arg_messageArgIsEmpty_throws() {
        new Diagnostic(Diagnostic.Level.FATAL, " ");
    }

    @Test
    public void constructor2arg_allArgsAreValid_returnsNewInstance() {
        final Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message");
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(is(nullValue())));
    }

    @Test(expected = NullPointerException.class)
    public void constructor3arg_levelArgIsNull_throws() {
        new Diagnostic(null, "message", new IllegalStateException());
    }

    @Test(expected = NullPointerException.class)
    public void constructor3arg_messageArgIsNull_throws() {
        new Diagnostic(Diagnostic.Level.FATAL, null, new IllegalStateException());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor3arg_messageArgIsEmpty_throws() {
        new Diagnostic(Diagnostic.Level.FATAL, " ", new IllegalStateException());
    }

    @Test
    public void constructor3arg_allArgsAreValid_returnsNewInstance() {
        final Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", new IllegalStateException());
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(is(notNullValue())));
    }

    @Test
    public void constructor3arg_stacktraceArgIsNull_returnsNewInstance() {
        final Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", null);
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(is(nullValue())));
    }

    @Test
    public void constructor2arg_instance_canBeMarshalledUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message");
        final String marshalled = jsonbContext.marshall(diagnostic);
        final Diagnostic unmarshalled = jsonbContext.unmarshall(marshalled, Diagnostic.class);
        assertThat("unmarshalled", unmarshalled, is(notNullValue()));
        assertThat("unmarshalled.level", unmarshalled.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("unmarshalled.message", unmarshalled.getMessage(), is("message"));
        assertThat("unmarshalled.stacktrace", unmarshalled.getStacktrace(), is(is(nullValue())));
    }

    @Test
    public void constructor3arg_instance_canBeMarshalledUnmarshalled() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", new IllegalStateException());
        final String marshalled = jsonbContext.marshall(diagnostic);
        final Diagnostic unmarshalled = jsonbContext.unmarshall(marshalled, Diagnostic.class);
        assertThat("unmarshalled", unmarshalled, is(notNullValue()));
        assertThat("unmarshalled.level", unmarshalled.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("unmarshalled.message", unmarshalled.getMessage(), is("message"));
        assertThat("unmarshalled.stacktrace", unmarshalled.getStacktrace(), is(is(notNullValue())));
    }
}