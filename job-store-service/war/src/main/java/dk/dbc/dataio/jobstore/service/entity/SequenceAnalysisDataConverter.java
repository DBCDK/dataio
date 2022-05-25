package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.jobstore.types.SequenceAnalysisData;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

@Converter
public class SequenceAnalysisDataConverter implements AttributeConverter<SequenceAnalysisData, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(SequenceAnalysisData sequenceAnalysisData) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("json");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(sequenceAnalysisData));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public SequenceAnalysisData convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), SequenceAnalysisData.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
