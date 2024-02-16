package dk.dbc.dataio.jobstore.distributed.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

@Converter
public class KeySetJSONBConverter implements AttributeConverter<Set<TrackingKey>, PGobject> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    final CollectionType JSONSetKeyType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(Set.class, TrackingKey.class);

    @Override
    public PGobject convertToDatabaseColumn(Set<TrackingKey> sequenceAnalysisData) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(OBJECT_MAPPER.writeValueAsString(sequenceAnalysisData));
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public Set<TrackingKey> convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            if (pgObject == null) return new LinkedHashSet<>();
            return OBJECT_MAPPER.readValue(pgObject.getValue(), JSONSetKeyType);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
