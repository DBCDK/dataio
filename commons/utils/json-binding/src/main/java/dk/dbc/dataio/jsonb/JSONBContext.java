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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.IOException;
import java.io.StringWriter;

/**
 * The JSONBContext class provides the client's entry point to the JSONB API with binding framework operations
 * unmarshal and marshal.
 * <p>
 * This class is thread safe.
 * </p>
 */
public class JSONBContext {
    private final ObjectMapper objectMapper;

    public JSONBContext() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Marshalls given value type into its corresponding JSON string representation
     * @param object object to marshall
     * @return JSON representation
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    public String marshall(Object object) throws JSONBException {
        final StringWriter stringWriter = new StringWriter();
        try {
            objectMapper.writeValue(stringWriter, object);
        } catch (IOException e) {
            throw new JSONBException(String.format(
                    "Exception caught when trying to marshall %s object to JSON", object.getClass().getName()), e);
        }
        return stringWriter.toString();
    }

    /**
     * Marshalls given value type into corresponding JSON string representation using using specified withType,
     * instead of actual runtime type of object
     * @param object object to marshall
     * @param withType root type
     * @return JSON representation
     * @throws JSONBException if unable to marshall value type into its JSON representation
     */
    public String marshall(Object object, JavaType withType) throws JSONBException {
        final StringWriter stringWriter = new StringWriter();
        try {
            new ObjectMapper().writerWithType(withType).writeValue(stringWriter, object);
        } catch (IOException e) {
            throw new JSONBException(String.format(
                    "Exception caught when trying to marshall %s object to JSON", object.getClass().getName()), e);
        }
        return stringWriter.toString();
    }

    /**
     * Unmarshalls JSON string into value type
     * @param json JSON representation of value type
     * @param tClass value type class
     * @param <T> type parameter
     * @return value type instance
     * @throws JSONBException if unable to unmarshall JSON representation to value type
     */
    public <T> T unmarshall(String json, Class<T> tClass) throws JSONBException {
        InvariantUtil.checkNotNullOrThrow(tClass, "tClass");
        try {
            return objectMapper.readValue(json, tClass);
        } catch (IOException e) {
            throw new JSONBException(String.format(
                    "Exception caught when trying to unmarshall JSON %s to %s object", json, tClass.getName()), e);
        }
    }

    /**
     * Unmarshalls JSON string into value type
     * @param json JSON representation of value type
     * @param toType value type representation
     * @param <T> type parameter
     * @return value type instance
     * @throws JSONBException if unable to unmarshall JSON representation to value type
     */
    public <T> T unmarshall(String json, JavaType toType) throws JSONBException {
        InvariantUtil.checkNotNullOrThrow(toType, "toType");
        try {
            return (T) objectMapper.readValue(json, toType);
        } catch (IOException e) {
            throw new JSONBException(String.format(
                    "Exception caught when trying to unmarshall JSON %s to %s object", json, toType), e);
        }
    }

    /**
     * Returns factory class for creating concrete java types.
     * @return TypeFactory instance
     */
    public TypeFactory getTypeFactory() {
        return objectMapper.getTypeFactory();
    }

    /**
     * @param json JSON document
     * @return root node of deserialized JSON content
     * @throws JSONBException if unable to unmarshall JSON into tree representation
     */
    public JsonNode getJsonTree(String json) throws JSONBException {
        try {
            return objectMapper.readValue(json, JsonNode.class);
        } catch (IOException e) {
            throw new JSONBException(String.format(
                    "Exception caught when trying to unmarshall JSON %s into JSON content tree", json), e);
        }
    }
}
