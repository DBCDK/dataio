package dk.dbc.dataio.filestore.service.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class String2JSonB implements AttributeConverter<String, PGobject> {

    @Override
    public PGobject convertToDatabaseColumn(String s) {
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        try {
            obj.setValue(s);
            return obj;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String convertToEntityAttribute(PGobject object) {
        if(object == null) return null;
        return object.getValue();
    }
}
