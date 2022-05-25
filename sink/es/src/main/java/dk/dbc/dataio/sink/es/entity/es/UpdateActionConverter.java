package dk.dbc.dataio.sink.es.entity.es;

import dk.dbc.dataio.sink.es.entity.es.TaskSpecificUpdateEntity.UpdateAction;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Created by ja7 on 02-10-15.
 * http://www.loc.gov/z3950/agency/defns/update-es-rev1.html
 * <p>
 * INSERT(1),
 * REPLACE(2),
 * DELETE(3),
 * ELEMENT_UPDATE(4),
 * SPECIAL_UPDATE(5);
 */
@Converter(autoApply = true)
public class UpdateActionConverter implements AttributeConverter<UpdateAction, Integer> {


    @Override
    public Integer convertToDatabaseColumn(UpdateAction updateAction) {
        switch (updateAction) {
            case INSERT:
                return 1;
            case REPLACE:
                return 2;
            case DELETE:
                return 3;
            case ELEMENT_UPDATE:
                return 4;
            case SPECIAL_UPDATE:
                return 5;
        }
        return null;
    }

    @Override
    public UpdateAction convertToEntityAttribute(Integer integer) {
        switch (integer) {
            case 1:
                return UpdateAction.INSERT;
            case 2:
                return UpdateAction.REPLACE;
            case 3:
                return UpdateAction.DELETE;
            case 4:
                return UpdateAction.ELEMENT_UPDATE;
            case 5:
                return UpdateAction.SPECIAL_UPDATE;
            default:
                throw new IllegalStateException("Unknown UpdatePackage Action in Database");
        }
    }
}
