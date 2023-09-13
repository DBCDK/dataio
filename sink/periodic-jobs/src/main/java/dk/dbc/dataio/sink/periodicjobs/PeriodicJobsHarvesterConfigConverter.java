package dk.dbc.dataio.sink.periodicjobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class PeriodicJobsHarvesterConfigConverter implements AttributeConverter<PeriodicJobsHarvesterConfig, PGobject> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public PGobject convertToDatabaseColumn(PeriodicJobsHarvesterConfig config) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(MAPPER.writeValueAsString(config));
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public PeriodicJobsHarvesterConfig convertToEntityAttribute(PGobject pgObject) {
        if (pgObject == null) {
            return null;
        }
        try {
            return MAPPER.readValue(pgObject.getValue(), PeriodicJobsHarvesterConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
