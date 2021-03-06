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
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ja7 on 27-09-14.
 * Entity Mapping for TaskpackageRecordStructure
 */
@Entity
@Table(name = "taskpackagerecordstructure")
@IdClass(SuppliedRecordsEntityPK.class)
public class TaskPackageRecordStructureEntity {
    @Id
    @Column(name = "targetreference", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer targetreference;
    @Id
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    public Integer lbnr;
    @Column(name = "recordstatus")
    @Convert(converter = RecordStatusConverter.class)
    public RecordStatus recordStatus;
    @Column(name = "recordorsurdiag2")
    public Integer diagnosticId;
    @Column(name = "record_id")
    public String record_id;

    /*
    https://en.wikibooks.org/wiki/Java_Persistence/OneToMany#Unidirectional_OneToMany.2C_No_Inverse_ManyToOne.2C_No_Join_Table_.28JPA_2.0_ONLY.29
    */

    private transient List<DiagnosticsEntity> diagnosticsEntities=new ArrayList<>();

    public List<DiagnosticsEntity> getDiagnosticsEntities( ) {
        if( diagnosticsEntities.isEmpty() && diagnosticId!=null) {
            throw new IllegalStateException("loadDiagnosticsEntities must be called before getDiagnosticesEntities");
        }
        return diagnosticsEntities;
    }

    public void setDiagnosticsEntities(List<DiagnosticsEntity> diagnosticsEntities) {
        this.diagnosticsEntities = diagnosticsEntities;
    }

    private void fetch_DiagnosticsEntityList(EntityManager em) {
        TypedQuery<DiagnosticsEntity> q = em.createNamedQuery("Diagnostics.findById", DiagnosticsEntity.class);
        q.setParameter("id", diagnosticId);
        diagnosticsEntities=q.getResultList();
    }

    public void loadDiagnosticsEntities(EntityManager entityManager) {
        fetch_DiagnosticsEntityList( entityManager );
    }


    //*/
    public enum RecordStatus {SUCCESS, QUEUED, IN_PROCESS, FAILURE}
}
