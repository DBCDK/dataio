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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Created by ja7 on 25-11-14.
 * standard Entity for diagnostics the NamedQuery is used by
 * TaskPackageRecordStructureEntity
 */
@Entity
@Table(name = "diagnostics")
@IdClass(DiagnosticsEntityPK.class)
@NamedQueries({
        @NamedQuery(name = "Diagnostics.findById",
                query = "select d from DiagnosticsEntity d where d.id=:id order by d.lbnr asc "
        ),
        @NamedQuery(name = "Diagnostics.findMaxLbNr",
                query = "select max(d.lbnr) from  DiagnosticsEntity d where d.id=:id"
        )
})
public class DiagnosticsEntity {
    @Id
    @Column(name = "id")
    public Integer id;
    @Id
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer lbnr;

    @Column(name = "diagnosticsetid")
    public String diagnosticSetId = "1.2.840.10003.4.1";
    @Column(name = "condition")
    public Integer errorCode = 100;
    @Column(name = "addinfo")
    public String additionalInformation="";


    public DiagnosticsEntity() {
        this.lbnr = 0;
    }

    public DiagnosticsEntity(int lbnr, String additionalInformation) {
        this.lbnr = lbnr;
        this.additionalInformation = additionalInformation;
    }

    public DiagnosticsEntity(int id, int lbnr, String additionalInformation) {
        this.id = id;
        this.lbnr = lbnr;
        this.additionalInformation = additionalInformation;
    }


    @Override
    public String toString() {
        return "DiagnosticsEntity{" +
                "id=" + id +
                ", lbnr=" + lbnr +
                ", diagnosticSetId='" + diagnosticSetId + '\'' +
                ", errorCode=" + errorCode +
                ", additionalInformation='" + additionalInformation + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiagnosticsEntity that = (DiagnosticsEntity) o;

        if (additionalInformation != null ? !additionalInformation.equals(that.additionalInformation) : that.additionalInformation != null)
            return false;
        if (diagnosticSetId != null ? !diagnosticSetId.equals(that.diagnosticSetId) : that.diagnosticSetId != null)
            return false;
        if (errorCode != null ? !errorCode.equals(that.errorCode) : that.errorCode != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (lbnr != null ? !lbnr.equals(that.lbnr) : that.lbnr != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (lbnr != null ? lbnr.hashCode() : 0);
        result = 31 * result + (diagnosticSetId != null ? diagnosticSetId.hashCode() : 0);
        result = 31 * result + (errorCode != null ? errorCode.hashCode() : 0);
        result = 31 * result + (additionalInformation != null ? additionalInformation.hashCode() : 0);
        return result;
    }
}

