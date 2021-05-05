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

package dk.dbc.dataio.harvester.task.entity;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;
import java.util.List;

/**
 * Handles mapping from/to List&lt;AddiMetaData&gt; to/from PostgreSQL JSON type
 */
@Converter
public class AddiMetaDataListConverter implements AttributeConverter<List<AddiMetaData>, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(List<AddiMetaData> list) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(JSONB_CONTEXT.marshall(list));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public List<AddiMetaData> convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        if (pgObject != null) {
            try {
                final CollectionType collectionType = JSONB_CONTEXT.getTypeFactory()
                        .constructCollectionType(List.class, AddiMetaData.class);
                return JSONB_CONTEXT.unmarshall(pgObject.getValue(), collectionType);
            } catch (JSONBException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
}
