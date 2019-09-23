/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.marcconv;

import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

@Converter
public class ConversionParamConverter implements AttributeConverter<ConversionParam, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(ConversionParam conversionParam) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(JSONB_CONTEXT.marshall(conversionParam));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public ConversionParam convertToEntityAttribute(PGobject pgObject) {
        if (pgObject == null) {
            return null;
        }
        try {
            return JSONB_CONTEXT.unmarshall(pgObject.getValue(), ConversionParam.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
