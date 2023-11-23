package dk.dbc.dataio.commons.types;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DiagnosticTest {

    // 2 arguments constructor

    @Test
    public void constructor2arg_levelArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Diagnostic(null, "message"));
    }

    @Test
    public void constructor2arg_messageArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Diagnostic(Diagnostic.Level.FATAL, null));
    }

    @Test
    public void constructor2arg_messageArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(Diagnostic.Level.FATAL, " "));
    }

    @Test
    public void constructor2arg_allArgsAreValid_returnsNewInstance() {
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message");
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(nullValue()));
        assertThat("diagnostic.tag", diagnostic.getTag(), is(nullValue()));
        assertThat("diagnostic.attribute", diagnostic.getAttribute(), is(nullValue()));
    }

    // 3 arguments constructor with a cause argument

    @Test
    public void constructorWith3ArgCause_levelArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Diagnostic(null, "message", new IllegalStateException()));
    }

    @Test
    public void constructorWith3ArgCause_messageArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Diagnostic(Diagnostic.Level.FATAL, null, new IllegalStateException()));
    }

    @Test
    public void constructorWith3ArgCause_messageArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(Diagnostic.Level.FATAL, " ", new IllegalStateException()));
    }

    @Test
    public void constructorWith3ArgCause_causeArgIsNull_returnsNewInstance() {
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", null);
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(nullValue()));
        assertThat("diagnostic.tag", diagnostic.getTag(), is(nullValue()));
        assertThat("diagnostic.attribute", diagnostic.getAttribute(), is(nullValue()));
    }

    @Test
    public void constructorWith3ArgCause_allArgsAreValid_returnsNewInstance() {
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", new IllegalStateException());
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(notNullValue()));
        assertThat("diagnostic.tag", diagnostic.getTag(), is(nullValue()));
        assertThat("diagnostic.attribute", diagnostic.getAttribute(), is(nullValue()));
    }

    // 5 arguments constructor

    @Test
    public void constructorWith5ArgStackTrace_levelArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Diagnostic(null, "message", "stacktrace", "tag", "attribute"));
    }

    @Test
    public void constructorWith5ArgStackTrace_messageArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new Diagnostic(Diagnostic.Level.FATAL, null, "stacktrace", "tag", "attribute"));
    }

    @Test
    public void constructorWith5ArgStackTrace_messageArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Diagnostic(Diagnostic.Level.FATAL, " ", "stacktrace", "tag", "attribute"));
    }

    @Test
    public void constructorWith5ArgStackTrace_stackTraceAndTagAndAttributeArgsAreNull_returnsNewInstance() {
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", null, null, null);
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(nullValue()));
        assertThat("diagnostic.tag", diagnostic.getTag(), is(nullValue()));
        assertThat("diagnostic.attribute", diagnostic.getAttribute(), is(nullValue()));
    }

    @Test
    public void constructorWith5ArgStackTrace_allArgsAreValid_returnsNewInstance() {
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", "stacktrace", "tag", "attribute");
        assertThat("diagnostic", diagnostic, is(notNullValue()));
        assertThat("diagnostic.level", diagnostic.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("diagnostic.message", diagnostic.getMessage(), is("message"));
        assertThat("diagnostic.stacktrace", diagnostic.getStacktrace(), is(notNullValue()));
        assertThat("diagnostic.tag", diagnostic.getTag(), is("tag"));
        assertThat("diagnostic.attribute", diagnostic.getAttribute(), is("attribute"));
    }

    // Marshall and unmarshall tests

    @Test
    public void constructor2arg_instance_canBeMarshalledUnmarshalled() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message");
        String marshalled = jsonbContext.marshall(diagnostic);
        Diagnostic unmarshalled = jsonbContext.unmarshall(marshalled, Diagnostic.class);
        assertThat("unmarshalled", unmarshalled, is(notNullValue()));
        assertThat("unmarshalled.level", unmarshalled.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("unmarshalled.message", unmarshalled.getMessage(), is("message"));
        assertThat("unmarshalled.stacktrace", unmarshalled.getStacktrace(), is(nullValue()));
        assertThat("unmarshalled.tag", unmarshalled.getTag(), is(nullValue()));
        assertThat("unmarshalled.attribute", unmarshalled.getAttribute(), is(nullValue()));
    }

    @Test
    public void constructorWith3ArgCause_instance_canBeMarshalledUnmarshalled() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", new IllegalStateException());
        String marshalled = jsonbContext.marshall(diagnostic);
        Diagnostic unmarshalled = jsonbContext.unmarshall(marshalled, Diagnostic.class);
        assertThat("unmarshalled", unmarshalled, is(notNullValue()));
        assertThat("unmarshalled.level", unmarshalled.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("unmarshalled.message", unmarshalled.getMessage(), is("message"));
        assertThat("unmarshalled.stacktrace", unmarshalled.getStacktrace(), is(notNullValue()));
        assertThat("unmarshalled.tag", unmarshalled.getTag(), is(nullValue()));
        assertThat("unmarshalled.attribute", unmarshalled.getAttribute(), is(nullValue()));
    }

    @Test
    public void constructorWith5Args_instance_canBeMarshalledUnmarshalled() throws JSONBException {
        JSONBContext jsonbContext = new JSONBContext();
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Level.FATAL, "message", "stacktrace", "tag", "attribute");
        String marshalled = jsonbContext.marshall(diagnostic);
        Diagnostic unmarshalled = jsonbContext.unmarshall(marshalled, Diagnostic.class);
        assertThat("unmarshalled", unmarshalled, is(notNullValue()));
        assertThat("unmarshalled.level", unmarshalled.getLevel(), is(Diagnostic.Level.FATAL));
        assertThat("unmarshalled.message", unmarshalled.getMessage(), is("message"));
        assertThat("unmarshalled.stacktrace", unmarshalled.getStacktrace(), is("stacktrace"));
        assertThat("unmarshalled.tag", unmarshalled.getTag(), is("tag"));
        assertThat("unmarshalled.attribute", unmarshalled.getAttribute(), is("attribute"));
    }

}
