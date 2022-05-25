package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.JobSpecification;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

/**
 * Handles mapping from/to JobSpecification to/from PostgreSQL JSON type
 */
@Converter
public class JobSpecificationConverter implements AttributeConverter<JobSpecification, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(JobSpecification jobSpecification) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(jobSpecification));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public JobSpecification convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), JobSpecification.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
