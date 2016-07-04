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

package dk.dbc.dataio.jsonb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
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
    public void unmarshall_byClass_jsonArgIsNull_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(null, SimpleBean.class);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void unmarshall_byClass_jsonArgIsEmpty_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall("", SimpleBean.class);
            fail("No exception thrown");
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_byClass_tClassArgIsNull_throws() throws JSONBException {
        final String json = "{\"value\":42}";
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(json, (Class) null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void unmarshall_byClass_emptyJsonObjectToObjectOfTypeWithDefaultConstructor_returnsInstance() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        assertThat(jsonbContext.unmarshall("{}", SimpleBean.class), is(notNullValue()));
    }

    @Test
    public void unmarshall_byClass_emptyJsonObjectToObjectOfTypeWithoutDefaultConstructor_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall("{}", SimpleBeanWithoutDefaultConstructor.class);
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_byClass_jsonObjectToObjectOfTypeWithDefaultConstructor_returnsInstance() throws Exception {
        final int value = 42;
        final String json = String.format("{\"value\":%d}", value);
        final JSONBContext jsonbContext = new JSONBContext();
        final SimpleBean instance = jsonbContext.unmarshall(json, SimpleBean.class);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getValue(), is(value));
    }

    @Test
    public void unmarshall_byClass_jsonObjectToObjectOfTypeWithoutDefaultConstructor_throws() throws Exception {
        final String json = "{\"value\":42}";
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(json, SimpleBeanWithoutDefaultConstructor.class);
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_byClass_jsonObjectToObjectOfTypeAnnotatedWithoutDefaultConstructor_returnsInstance() throws Exception {
        final int value = 42;
        final String json = String.format("{\"value\":%d}", value);
        final JSONBContext jsonbContext = new JSONBContext();
        final AnnotatedSimpleBeanWithoutDefaultConstructor instance =
                jsonbContext.unmarshall(json, AnnotatedSimpleBeanWithoutDefaultConstructor.class);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getValue(), is(value));
    }

    @Test
    public void unmarshall_byJavaType_jsonArgIsNull_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(null, jsonbContext.getTypeFactory().constructType(SimpleBean.class));
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void unmarshall_byJavaType_jsonArgIsEmpty_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall("", jsonbContext.getTypeFactory().constructType(SimpleBean.class));
            fail("No exception thrown");
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_byJavaType_toTypeArgIsNull_throws() throws JSONBException {
        final String json = "{\"value\":42}";
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(json, (JavaType) null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void unmarshall_byJavaType_emptyJsonObjectToObjectOfTypeWithDefaultConstructor_returnsInstance() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        assertThat(jsonbContext.unmarshall("{}", jsonbContext.getTypeFactory().constructType(SimpleBean.class)), is(notNullValue()));
    }

    @Test
    public void unmarshall_byJavaType_emptyJsonObjectToObjectOfTypeWithoutDefaultConstructor_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall("{}", jsonbContext.getTypeFactory().constructType(SimpleBeanWithoutDefaultConstructor.class));
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_byJavaType_jsonObjectToObjectOfTypeWithDefaultConstructor_returnsInstance() throws Exception {
        final int value = 42;
        final String json = String.format("{\"value\":%d}", value);
        final JSONBContext jsonbContext = new JSONBContext();
        final SimpleBean instance = jsonbContext.unmarshall(json, jsonbContext.getTypeFactory().constructType(SimpleBean.class));
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getValue(), is(value));
    }

    @Test
    public void unmarshall_byJavaType_jsonObjectToObjectOfTypeWithoutDefaultConstructor_throws() throws Exception {
        final String json = "{\"value\":42}";
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.unmarshall(json, jsonbContext.getTypeFactory().constructType(SimpleBeanWithoutDefaultConstructor.class));
        } catch (JSONBException e) {
        }
    }

    @Test
    public void unmarshall_byJavaType_jsonObjectToObjectOfTypeAnnotatedWithoutDefaultConstructor_returnsInstance() throws Exception {
        final int value = 42;
        final String json = String.format("{\"value\":%d}", value);
        final JSONBContext jsonbContext = new JSONBContext();
        final AnnotatedSimpleBeanWithoutDefaultConstructor instance =
                jsonbContext.unmarshall(json, jsonbContext.getTypeFactory().constructType(AnnotatedSimpleBeanWithoutDefaultConstructor.class));
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getValue(), is(value));
    }

    @Test
    public void getJsonTree_jsonArgIsNull_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.getJsonTree(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getJsonTree_jsonArgIsEmpty_throws() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        try {
            jsonbContext.getJsonTree("");
            fail("No exception thrown");
        } catch (JSONBException e) {
        }
    }

    @Test
    public void getJsonTree_emptyJsonDocument_returnsRootNode() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final JsonNode jsonNode = jsonbContext.getJsonTree("{}");
        assertThat(jsonNode, is(notNullValue()));
    }

    @Test
    public void getJsonTree_nonEmptyJsonDocument_returnsRootNode() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final JsonNode jsonNode = jsonbContext.getJsonTree("{\"key\":\"value\"}");
        assertThat(jsonNode, is(notNullValue()));
    }

    @Test
    public void prettyPrint_jsonStringIsValid_returnsPrettyPrintedJsonString() throws JSONBException {
        final JSONBContext jsonbContext = new JSONBContext();
        final String prettyPrint = jsonbContext.prettyPrint("{\"value\":42}");
        assertThat(prettyPrint, is("{\n  \"value\" : 42\n}"));
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