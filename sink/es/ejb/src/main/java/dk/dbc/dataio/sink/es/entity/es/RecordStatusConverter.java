/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.sink.es.entity.es;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import static dk.dbc.dataio.sink.es.entity.es.TaskPackageRecordStructureEntity.RecordStatus;


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
                return RecordStatus.SUCCESS;
            case 2:
                return RecordStatus.QUEUED;
            case 3:
                return RecordStatus.IN_PROCESS;
            case 4:
                return RecordStatus.FAILURE;
            default:
        }
        return null;
    }


}
