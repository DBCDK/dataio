package dk.dbc.dataio.jobstore.service.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

@Converter
public class RerunStateConverter implements AttributeConverter<RerunEntity.State, Object> {
    @Override
    public Object convertToDatabaseColumn(RerunEntity.State state) {
        if (state == null) {
            state = RerunEntity.State.WAITING;
        }
        final PGobject pgObject = new PGobject();
        pgObject.setType("rerun_state");
        try {
            pgObject.setValue(state.name());
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public RerunEntity.State convertToEntityAttribute(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("database object can not be null");
        }
        switch ((String) o) {
            case "IN_PROGRESS":
                return RerunEntity.State.IN_PROGRESS;
            case "WAITING":
                return RerunEntity.State.WAITING;
            default:
                return null;
        }
    }
}
