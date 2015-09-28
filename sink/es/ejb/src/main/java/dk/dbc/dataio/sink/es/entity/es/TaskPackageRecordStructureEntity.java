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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import java.math.BigInteger;
import java.util.List;

/**
 * Created by ja7 on 27-09-14.
 * Entity Mapping for TaskpackageRecordStructure
 */
@Entity
@Table(name = "taskpackagerecordstructure")
@IdClass(SuppliedRecordsEntityPK.class)
public class TaskPackageRecordStructureEntity {
    private static final Logger log = LoggerFactory.getLogger(TaskPackageRecordStructureEntity.class);
    @Id
    @Column(name = "targetreference", nullable = false, insertable = true, updatable = true, precision = 0)
    public BigInteger targetreference;
    @Id
    @Column(name = "lbnr", nullable = false, insertable = true, updatable = true, precision = 0)
    public BigInteger lbnr;
    @Column(name = "recordstatus")
    @Convert(converter = RecordStatusConverter.class)
    public RecordStatus recordStatus;
    @Column(name = "recordorsurdiag2")
    public Integer diagnosticId;

    public List<DiagnosticsEntity> getDiagnosticsEntityList(EntityManager em) {
        TypedQuery<DiagnosticsEntity> q = em.createNamedQuery("Diagnostics.findById", DiagnosticsEntity.class);
        q.setParameter("id", diagnosticId);
        return q.getResultList();

    }

    public void appendDiagnostics(String additionalInfo, EntityManager em) {
        DiagnosticsEntity diagnosticsEntity = new DiagnosticsEntity(0, additionalInfo);
        Integer nextLbnr = 0;
        if (diagnosticId != null) {
            TypedQuery<Integer> q = em.createNamedQuery("Diagnostics.findMaxLbNr", Integer.class);
            q.setParameter("id", diagnosticId);
            Integer curMaxLbnr = q.getSingleResult();
            if (curMaxLbnr != null) {
                nextLbnr = curMaxLbnr + 1;
            }
            diagnosticsEntity.id = diagnosticId;
        }

        diagnosticsEntity.lbNr = nextLbnr;
        em.persist(diagnosticsEntity);
        diagnosticId = diagnosticsEntity.id;
        recordStatus = RecordStatus.FAILURE;
        log.trace("appended diagnostics id {} lbNr {} to tref {} lbNr", diagnosticsEntity.id, diagnosticsEntity.lbNr, targetreference, lbnr);
    }

    /**
     * Test Tp se if the Taskpackage is Complete.
     *
     * @return True if the Taskpackage has Completed
     */
    public boolean isRecordProcessingComplete() {
        switch (recordStatus) {
            case QUEUED:
            case IN_PROCESS:
                return false;
        }
        return true;
    }

    public enum RecordStatus {SUCCESS, QUEUED, IN_PROCESS, FAILURE}
}
