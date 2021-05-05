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

package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Flow;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

/**
 * Handles mapping from/to Flow to/from PostgreSQL JSON type
 */
@Converter
public class FlowConverter implements AttributeConverter<Flow, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(Flow flow) throws IllegalStateException {
        try {
            return convertToDatabaseColumn(ConverterJSONBContext.getInstance().marshall(flow));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Flow convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), Flow.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    public PGobject convertToDatabaseColumn(String flow) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("json");
        try {
            pgObject.setValue(flow);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }
}
