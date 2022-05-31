package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

@Converter
public class PeriodicJobsHarvesterConfigConverter implements AttributeConverter<PeriodicJobsHarvesterConfig, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(PeriodicJobsHarvesterConfig config) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(JSONB_CONTEXT.marshall(config));
        } catch (SQLException | JSONBException e) {
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
            return JSONB_CONTEXT.unmarshall(pgObject.getValue(), PeriodicJobsHarvesterConfig.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
