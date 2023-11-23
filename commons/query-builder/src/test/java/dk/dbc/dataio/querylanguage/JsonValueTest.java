package dk.dbc.dataio.querylanguage;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonValueTest {
    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void emptyValue() throws JSONBException {
        JsonValue jsonValue = new JsonValue();
        assertThat("isEmpty()", jsonValue.isEmpty(), is(true));
        assertThat("JSON", jsonbContext.marshall(jsonValue), is("{}"));
    }

    @Test
    public void put() throws JSONBException {
        JsonValue jsonValue = new JsonValue()
                .put("testProperty", "before replace");
        assertThat("JSON", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":\"before replace\"}"));
        jsonValue.put("testProperty", "after replace");
        assertThat("JSON", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":\"after replace\"}"));
    }

    @Test
    public void hasProperty() {
        JsonValue jsonValue = new JsonValue()
                .put("testProperty", "test value");
        assertThat("hasProperty for existing", jsonValue.hasProperty("testProperty"),
                is(true));
        assertThat("hasProperty for non-existing", jsonValue.hasProperty("unknownProperty"),
                is(false));
    }

    @Test
    public void remove() throws JSONBException {
        JsonValue jsonValue = new JsonValue()
                .put("testProperty", "test property");
        assertThat("JSON before remove", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":\"test property\"}"));
        assertThat("remove", jsonValue.remove("testProperty"),
                is("test property"));
        assertThat("JSON after remove", jsonbContext.marshall(jsonValue),
                is("{}"));
        assertThat("remove non-existing", jsonValue.remove("testProperty"),
                is(nullValue()));
    }

    @Test
    public void add() throws JSONBException {
        JsonValue jsonValue = new JsonValue()
                .add("testProperty", 1)
                .add("testProperty", 2)
                .add("testProperty", 3)
                .add("another", 42);
        assertThat("JSON after remove", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":[1,2,3],\"another\":[42]}"));
    }

    @Test
    public void addThrowsOnNonArrayProperty() {
        assertThrows(IllegalArgumentException.class, () -> new JsonValue()
                .put("testProperty", 1)
                .add("testProperty", 2));
    }

    @Test
    public void removeArrayElement() throws JSONBException {
        JsonValue jsonValue = new JsonValue()
                .add("testProperty", 1)
                .add("testProperty", 2)
                .add("testProperty", 3)
                .add("testProperty", 2);
        assertThat("JSON before remove", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":[1,2,3,2]}"));
        assertThat("1st remove", jsonValue.removeArrayElement("testProperty", 2),
                is(true));
        assertThat("JSON after 1st remove", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":[1,3,2]}"));
        assertThat("2nd remove", jsonValue.removeArrayElement("testProperty", 2),
                is(true));
        assertThat("JSON after 2nd remove", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":[1,3]}"));
        assertThat("3rd remove", jsonValue.removeArrayElement("testProperty", 2),
                is(false));
        assertThat("JSON after 3rd remove", jsonbContext.marshall(jsonValue),
                is("{\"testProperty\":[1,3]}"));
    }

    @Test
    public void removeArrayElementThrowsOnNonArrayProperty() {
        JsonValue jsonValue = new JsonValue()
                .put("testProperty", 1);
        assertThrows(IllegalArgumentException.class, () -> jsonValue.removeArrayElement("testProperty", 0));
    }
}
