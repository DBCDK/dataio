package dk.dbc.dataio.jsonb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class JSONBContextTest {
    @Test
    public void marshall_objectCanNotBeMarshalled_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.marshall(new Object());
            fail("No exception thrown");
        } catch (JSONBException e) {
        }
    }

    @Test
    public void marshall_objectIsNull_returnsRepresentation() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        assertThat(jsonbContext.marshall(null), is("null"));
    }

    @Test
    public void marshall_objectOfTypeWithDefaultConstructorToJsonObject_returnsRepresentation() throws JSONBException {
        final int value = 42;
        final String expectedStringRepresentation = String.format("{\"value\":%d}", value);
        final SimpleBean object = new SimpleBean();
        object.setValue(value);

        final JSONBContext jsonbContext = new JSONBContext();
        assertThat(jsonbContext.marshall(object), is(expectedStringRepresentation));
    }

    @Test
    public void marshall_objectOfTypeWithoutDefaultConstructorToJsonObject_returnsRepresentation() throws JSONBException {
        final int value = 42;
        final String expectedStringRepresentation = String.format("{\"value\":%d}", value);
        final SimpleBeanWithoutDefaultConstructor object = new SimpleBeanWithoutDefaultConstructor(value);

        final JSONBContext jsonbContext = new JSONBContext();
        assertThat(jsonbContext.marshall(object), is(expectedStringRepresentation));
    }

    @Test
    public void unmarshall_jsonArgIsNull_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(null, SimpleBean.class);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void unmarshall_jsonArgIsEmpty_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall("", SimpleBean.class);
            fail("No exception thrown");
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_tClassArgIsNull_throws() throws JSONBException {
        final String json = "{\"value\":42}";
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(json, null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void unmarshall_emptyJsonObjectToObjectOfTypeWithDefaultConstructor_returnsInstance() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        assertThat(jsonbContext.unmarshall("{}", SimpleBean.class), is(notNullValue()));
    }

    @Test
    public void unmarshall_emptyJsonObjectToObjectOfTypeWithoutDefaultConstructor_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall("{}", SimpleBeanWithoutDefaultConstructor.class);
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_jsonObjectToObjectOfTypeWithDefaultConstructor_returnsInstance() throws Exception {
        final int value = 42;
        final String json = String.format("{\"value\":%d}", value);
        final JSONBContext jsonbContext = new JSONBContext();
        final SimpleBean instance = jsonbContext.unmarshall(json, SimpleBean.class);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getValue(), is(value));
    }

    @Test
    public void unmarshall_jsonObjectToObjectOfTypeWithoutDefaultConstructor_throws() throws Exception {
        final String json = "{\"value\":42}";
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(json, SimpleBeanWithoutDefaultConstructor.class);
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_jsonObjectToObjectOfTypeAnnotatedWithoutDefaultConstructor_returnsInstance() throws Exception {
        final int value = 42;
        final String json = String.format("{\"value\":%d}", value);
        final JSONBContext jsonbContext = new JSONBContext();
        final AnnotatedSimpleBeanWithoutDefaultConstructor instance =
                jsonbContext.unmarshall(json, AnnotatedSimpleBeanWithoutDefaultConstructor.class);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getValue(), is(value));
    }

    private static class SimpleBean {
        private int value;
        public int getValue() {
            return value;
        }
        public void setValue(int value) {
            this.value = value;
        }
    }

    private static class SimpleBeanWithoutDefaultConstructor {
        private final int value;
        public SimpleBeanWithoutDefaultConstructor(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    private static class AnnotatedSimpleBeanWithoutDefaultConstructor {
        private final int value;
        @JsonCreator
        public AnnotatedSimpleBeanWithoutDefaultConstructor(@JsonProperty("value") int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
}