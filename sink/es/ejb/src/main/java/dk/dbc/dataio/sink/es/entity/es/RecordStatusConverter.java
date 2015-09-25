package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity.RecordStatus;
import static dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity.RecordStatus.SUCCESS;
import static dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity.RecordStatus.QUEUED;
import static dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity.RecordStatus.IN_PROCESS;
import static dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity.RecordStatus.FAILURE;


/**
 * Created by ja7 on 05-10-14.
 * JPA Converter Class for enum RecordStatus
 *
           recordStatus        [3] IMPLICIT INTEGER{
                                    success      (1),
                                    queued       (2),
                                    inProcess    (3),
                                    failure      (4)}}

 */
@Converter(autoApply = true)
public class RecordStatusConverter  implements AttributeConverter<RecordStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(TaskPackageRecordStructureEntity.RecordStatus recordStatus) {
        switch ( recordStatus ) {
            case SUCCESS:
                return 1;
            case QUEUED:
                return 2;
            case IN_PROCESS:
                return 3;
            case FAILURE:
                return 4;
            default:
        }
        return null;
    }

    @Override
    public RecordStatus convertToEntityAttribute(Integer integer) {
        switch ( integer ) {
            case 1:
                return SUCCESS;
            case 2:
                return QUEUED;
            case 3:
                return IN_PROCESS;
            case 4:
                return FAILURE;
            default:
        }
        return null;
    }


}
