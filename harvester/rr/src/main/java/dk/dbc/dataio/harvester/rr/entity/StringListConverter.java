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

package dk.dbc.dataio.harvester.rr.entity;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.dataio.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;
import java.util.List;

/**
 * Handles mapping from/to List&lt;String&gt; to/from PostgreSQL JSON type
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(List<String> list) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(list));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public List<String> convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            final CollectionType collectionType = ConverterJSONBContext.getInstance().getTypeFactory()
                    .constructCollectionType(List.class, String.class);
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), collectionType);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}