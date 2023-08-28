package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class ChunkItemConverter implements AttributeConverter<ChunkItem, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(ChunkItem chunkItem) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(chunkItem));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public ChunkItem convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            if (pgObject != null) {
                return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), ChunkItem.class);
            }
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
