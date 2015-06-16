package dk.dbc.dataio.jobstore.types;

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