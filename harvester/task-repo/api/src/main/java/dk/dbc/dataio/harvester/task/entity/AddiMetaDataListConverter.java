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
