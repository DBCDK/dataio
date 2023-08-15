package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.jobstore.types.WorkflowNote;
import org.postgresql.util.PGobject;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.SQLException;

@Converter
public class WorkflowNoteConverter implements AttributeConverter<WorkflowNote, PGobject> {
    @Override
    public PGobject convertToDatabaseColumn(WorkflowNote workflowNote) throws IllegalStateException {
        final PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        try {
            if (workflowNote != null) {
                pgObject.setValue(ConverterJSONBContext.getInstance().marshall(workflowNote));
            } else {
                pgObject.setValue(null);
            }
        } catch (SQLException | JSONBException e) {
            throw new IllegalStateException(e);
        }
        return pgObject;
    }

    @Override
    public WorkflowNote convertToEntityAttribute(PGobject pgObject) throws IllegalStateException {
        try {
            if (pgObject != null) {
                return ConverterJSONBContext.getInstance().unmarshall(pgObject.getValue(), WorkflowNote.class);
            } else {
                return null;
            }
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
