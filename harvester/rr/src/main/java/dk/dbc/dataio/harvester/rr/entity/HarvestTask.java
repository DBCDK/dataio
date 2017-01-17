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

package dk.dbc.dataio.harvester.rr.entity;

import dk.dbc.dataio.commons.types.AddiMetaData;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "task")
@NamedQueries(
    @NamedQuery(name = HarvestTask.QUERY_FIND_READY,
        query = "SELECT task FROM HarvestTask task WHERE task.configId = :configId AND task.status = dk.dbc.dataio.harvester.rr.entity.HarvestTask.Status.READY ORDER BY task.id ASC")
)
public class HarvestTask {
    public static final String QUERY_FIND_READY = "HarvestTask.findReady";

    public enum Status {
        WAITING_TO_EXPAND,  // waiting to be split up into sub tasks
        WAITING,            // waiting to enter ready state
        READY,              // ready but not yet processed
        COMPLETED           // processed
    }

    @Id
    @SequenceGenerator(
            name = "task_id_seq",
            sequenceName = "task_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "task_id_seq")
    @Column(updatable = false)
    private int id;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;
    private Timestamp timeOfCompletion;

    private String tag;
    private Long configId;
    private Integer basedOnJob;
    private Integer numberOfRecords;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "json")
    @Convert(converter = AddiMetaDataListConverter.class)
    private List<AddiMetaData> records;

    public HarvestTask() {}

    public int getId() {
        return id;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Timestamp getTimeOfCompletion() {
        return timeOfCompletion;
    }

    public void setTimeOfCompletion(Timestamp timeOfCompletion) {
        this.timeOfCompletion = timeOfCompletion;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Integer getBasedOnJob() {
        return basedOnJob;
    }

    public void setBasedOnJob(Integer basedOnJob) {
        this.basedOnJob = basedOnJob;
    }

    public Integer getNumberOfRecords() {
        return numberOfRecords;
    }

    public void setNumberOfRecords(Integer numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public List<AddiMetaData> getRecords() {
        return records;
    }

    public void setRecords(List<AddiMetaData> records) {
        this.records = records;
    }
}
