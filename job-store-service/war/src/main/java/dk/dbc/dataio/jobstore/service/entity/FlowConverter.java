package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.partioner.entity.ConverterJSONBContext;
import dk.dbc.dataio.commons.types.Flow;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

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
