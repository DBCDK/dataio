package dk.dbc.dataio.commons.partioner.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.jobstore.types.RecordInfo;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class RecordInfoConverter implements AttributeConverter<RecordInfo, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(RecordInfo recordInfo) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(recordInfo));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public RecordInfo convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            if (pgObject != null) {
                return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), RecordInfo.class);
            }
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
