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

package dk.dbc.dataio.sink.es.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@IdClass(value = EsInFlightPK.class)
@NamedQueries({
    @NamedQuery(name = EsInFlight.FIND_ALL, query = EsInFlight.QUERY_FIND_ALL)
})
public class EsInFlight {
    public static final String FIND_ALL = "EsInFlight.findAll";
    public static final String QUERY_PARAMETER_RESOURCENAME = "resourceName";
    public static final String QUERY_FIND_ALL =
            "SELECT esInFlight FROM EsInFlight esInFlight WHERE esInFlight.resourceName = :"
                    + QUERY_PARAMETER_RESOURCENAME;

    @Id
    @Lob
    private String resourceName;

    @Id
    private Integer targetReference;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Long chunkId;

    @Column(nullable = false)
    private Integer recordSlots;

    @Lob
    @Column(nullable = false)
    private String sinkChunkResult; // TODO: This should be changed, but requires reinstall of database together with change in source.

    public Long getChunkId() {
        return chunkId;
    }

    public void setChunkId(Long chunkId) {
        this.chunkId = chunkId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Integer getRecordSlots() {
        return recordSlots;
    }

    public void setRecordSlots(Integer recordSlots) {
        this.recordSlots = recordSlots;
    }

    public Integer getTargetReference() {
        return targetReference;
    }

    public void setTargetReference(Integer targetReference) {
        this.targetReference = targetReference;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getIncompleteDeliveredChunk() {
        return sinkChunkResult;
    }

    public void setIncompleteDeliveredChunk(String incompleteDeliveredChunk) {
        this.sinkChunkResult = incompleteDeliveredChunk;
    }
}
