package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.types.ItemData;
import dk.dbc.dataio.jsonb.JSONBException;
import org.postgresql.util.PGobject;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.SQLException;

@Converter
public class ItemDataConverter implements AttributeConverter<ItemData, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(ItemData itemData) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("json");
        try {
            pgObject.setValue(ConverterJSONBContext.getInstance().marshall(itemData));
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public ItemData convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), ItemData.class);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
