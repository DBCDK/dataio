package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.partioner.entity.ConverterJSONBContext;
import dk.dbc.dataio.commons.types.Sink;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

/**
 * Handles mapping from/to Sink to/from PostgreSQL JSON type
 */
@Converter
public class SinkConverter implements AttributeConverter<Sink, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(Sink sink) throws IllegalStateException {
        try {
            return convertToDatabaseColumn(ConverterJSONBContext.getInstance().marshall(sink));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Sink convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), Sink.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    public PGobject convertToDatabaseColumn(String sink) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("json");
        try {
            pgObject.setValue(sink);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }
}
