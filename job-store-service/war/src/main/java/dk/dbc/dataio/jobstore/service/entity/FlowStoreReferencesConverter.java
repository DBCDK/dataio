package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

/**
 * Handles mapping from/to FlowStoreReferences to/from PostgreSQL JSON type
 */
@Converter
public class FlowStoreReferencesConverter implements AttributeConverter<FlowStoreReferences, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(FlowStoreReferences flowStoreReferences) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("json");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(flowStoreReferences));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public FlowStoreReferences convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), FlowStoreReferences.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
