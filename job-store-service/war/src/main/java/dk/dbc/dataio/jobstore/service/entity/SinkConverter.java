package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

/**
 * Handles mapping from/to Sink to/from PostgreSQL JSON type
 */
@Converter
public class SinkConverter implements AttributeConverter<Sink, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(Sink sink) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("json");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(sink));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public Sink convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), Sink.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
