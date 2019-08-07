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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.util.Objects;

/**
 * Created by ja7 on 23-09-14.
 *
 * Entity Mapping for SuppliedRecords.. ignores originalrecord
 */
@Entity
@Table(name = "suppliedrecords")
@IdClass(SuppliedRecordsEntityPK.class)
public class SuppliedRecordsEntity {
    @Id
    @Column(name = "targetreference")
    public Integer targetreference;
    @Id
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer lbnr;
    @Column(name = "supplementalid3", nullable = true, insertable = true, updatable = true, length = 400)
    public String metaData;
    @Column(name = "record", nullable = false, insertable = true, updatable = true)
    public byte[] record;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SuppliedRecordsEntity)) return false;
        SuppliedRecordsEntity that = (SuppliedRecordsEntity) o;
        return Objects.equals(targetreference, that.targetreference) &&
                Objects.equals(lbnr, that.lbnr) &&
                Objects.equals(metaData, that.metaData) &&
                Objects.equals(record, that.record);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetreference, lbnr, metaData, record);
    }
}
