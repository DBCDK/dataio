package dk.dbc.dataio.flowstore.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * Handles mapping from/to String to/from PostgreSQL JSON type
 */
@Converter
public class JsonConverter implements AttributeConverter<String, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(String content) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(content);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public String convertToEntityAttribute(PGobject pgObject) {
        if (pgObject == null) {
            return null;
        }
        return pgObject.getValue();
    }
}

