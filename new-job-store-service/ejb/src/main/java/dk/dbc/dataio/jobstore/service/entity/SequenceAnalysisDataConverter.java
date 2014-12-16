package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;
import java.util.List;

@Converter
public class SequenceAnalysisDataConverter implements AttributeConverter<List, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(List sequenceAnalysisData) throws IllegalStateException {
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
    public List convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), List.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
