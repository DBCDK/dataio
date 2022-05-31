package dk.dbc.dataio.sink.marcconv;

import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

@Converter
public class ConversionParamConverter implements AttributeConverter<ConversionParam, PGobject> {
    private static final JSONBContext JSONB_CONTEXT = new JSONBContext();

    @Override
    public PGobject convertToDatabaseColumn(ConversionParam conversionParam) {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            pgObject.setValue(JSONB_CONTEXT.marshall(conversionParam));
        } catch (SQLException | JSONBException e) {
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
            return JSONB_CONTEXT.unmarshall(pgObject.getValue(), ConversionParam.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
