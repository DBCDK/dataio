package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

/**
 * Handles mapping from/to State to/from PostgreSQL JSON type
 */
@Converter
public class StateConverter implements AttributeConverter<State, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(State state) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("json");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(state));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public State convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), State.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
