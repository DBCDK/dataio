package dk.dbc.dataio.sink.marcconv.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class ConversionParamConverter implements AttributeConverter<ConversionParam, PGobject> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public PGobject convertToDatabaseColumn(ConversionParam conversionParam) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(MAPPER.writeValueAsString(conversionParam));
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public ConversionParam convertToEntityAttribute(PGobject pgObject) {
        if (pgObject == null) {
            return null;
        }
        try {
            return MAPPER.readValue(pgObject.getValue(), ConversionParam.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
