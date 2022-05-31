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
