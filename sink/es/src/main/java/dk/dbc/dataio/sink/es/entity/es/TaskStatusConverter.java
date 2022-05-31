package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity.TaskStatus;

/**
 * Created by ja7 on 06-10-14.
 * Handles mapping from TaskStatus To Database Value maped from ASN.1
 * http://www.loc.gov/z3950/agency/asn1.html#RecordSyntax-ESTaskPackage
 * <p>
 * taskStatus                [9]   IMPLICIT INTEGER{
 * pending  (0),
 * active   (1),
 * complete (2),
 * aborted  (3)},
 */
@Converter(autoApply = true)
public class TaskStatusConverter implements AttributeConverter<TaskStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TaskStatus taskStatus) {
        if (taskStatus == null) {
            return 0;
        }
        switch (taskStatus) {
            case PENDING:
                return 0;
            case ACTIVE:
                return 1;
            case COMPLETE:
                return 2;
            case ABORTED:
                return 3;
        }
        return null;
    }

    @Override
    public TaskStatus convertToEntityAttribute(Integer integer) {
        switch (integer) {
            case 0:
                return TaskStatus.PENDING;
            case 1:
                return TaskStatus.ACTIVE;
            case 2:
                return TaskStatus.COMPLETE;
            case 3:
                return TaskStatus.ABORTED;
        }
        return null;
    }
}
