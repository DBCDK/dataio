package dk.dbc.dataio.jobstore.distributed.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.util.Set;

@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, PGobject> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public PGobject convertToDatabaseColumn(Set<String> sequenceAnalysisData) throws IllegalStateException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(OBJECT_MAPPER.writeValueAsString(sequenceAnalysisData));
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public Set<String> convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return OBJECT_MAPPER.readValue(pgObject.getValue(), new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
