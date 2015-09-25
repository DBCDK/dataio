package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateStatus;
import static dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateStatus.FAILURE;
import static dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateStatus.PARTIAL;
import static dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateStatus.SUCCESS;
import static dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateStatus.UNKNOWN;

/**
 * Created by ja7 on 06-10-14.
 http://www.loc.gov/z3950/agency/asn1.html#ESFormat-Update
 updateStatus           [1] IMPLICIT INTEGER{
                              success   (1),
                              partial   (2),
                              failure   (3)},

 Note The database Schema, has The Value 0 as default, so

 */

@Converter(autoApply = true)
public class UpdateStatusConverter implements AttributeConverter<TaskSpecificUpdateEntity.UpdateStatus, Integer > {
    @Override
    public Integer convertToDatabaseColumn(TaskSpecificUpdateEntity.UpdateStatus updateStatus) {
        switch ( updateStatus) {
            case UNKNOWN: return 0;
            case SUCCESS: return 1;
            case PARTIAL: return 2;
            case FAILURE: return 3;
        }
        return null;
    }

    @Override
    public UpdateStatus convertToEntityAttribute(Integer integer) {
        switch( integer ) {
            case 0: return UNKNOWN;
            case 1: return SUCCESS;
            case 2: return PARTIAL;
            case 3: return FAILURE;
        }
        return null;
    }
}
