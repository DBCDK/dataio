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

import static dk.dbc.dataio.sink.es.entity.es.TaskPackageEntity.TaskStatus;

/**
 * Created by ja7 on 06-10-14.
 * Handles mapping from TaskStatus To Database Value maped from ASN.1
 * http://www.loc.gov/z3950/agency/asn1.html#RecordSyntax-ESTaskPackage
 *
 *    taskStatus                [9]   IMPLICIT INTEGER{
                                        pending  (0),
                                        active   (1),
                                        complete (2),
                                        aborted  (3)},
 *
 */
@Converter(autoApply = true)
public class TaskStatusConverter implements AttributeConverter<TaskStatus,Integer>
{

    @Override
    public Integer convertToDatabaseColumn(TaskStatus taskStatus) {
        if (taskStatus == null) {
            return 0;
        }
        switch ( taskStatus ) {
            case PENDING: return 0;
            case ACTIVE: return 1;
            case COMPLETE: return 2;
            case ABORTED: return 3;
        }
        return null;
    }

    @Override
    public TaskStatus convertToEntityAttribute(Integer integer) {
        switch ( integer ) {
            case 0: return TaskStatus.PENDING;
            case 1: return TaskStatus.ACTIVE;
            case 2: return TaskStatus.COMPLETE;
            case 3: return TaskStatus.ABORTED;
        }
        return null;
    }
}
