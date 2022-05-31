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

package dk.dbc.dataio.jobstore.service.entity;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "rerun")
@NamedQueries({
        @NamedQuery(name = RerunEntity.FIND_HEAD_QUERY_NAME,
                query = "SELECT rerun FROM RerunEntity rerun ORDER BY rerun.id ASC"),

        @NamedQuery(name = RerunEntity.FIND_BY_STATE_QUERY_NAME,
                query = "SELECT rerun FROM RerunEntity rerun WHERE rerun.state = :" + RerunEntity.FIELD_STATE),
})
public class RerunEntity {
    public static final String FIND_HEAD_QUERY_NAME = "RerunEntity.findHead";
    public static final String FIND_BY_STATE_QUERY_NAME = "RerunEntity.findByState";
    public static final String FIELD_STATE = "state";

    public enum State {IN_PROGRESS, WAITING}

    @Id
    @SequenceGenerator(
            name = "rerun_id_seq",
            sequenceName = "rerun_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "rerun_id_seq")
    @Column(updatable = false)
    private int id;

    @Convert(converter = RerunStateConverter.class)
    private State state;

    @Column(updatable = false)
    private Timestamp timeOfCreation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="jobId", updatable = false)
    private JobEntity job;

    private Boolean includeFailedOnly;

    public RerunEntity() {}

    public int getId() {
        return id;
    }

    public RerunEntity withId(int id) {
        this.id = id;
        return this;
    }

    public JobEntity getJob() {
        return job;
    }

    public RerunEntity withJob(JobEntity job) {
        this.job = job;
        return this;
    }

    public State getState() {
        return state;
    }

    public RerunEntity withState(State state) {
        this.state = state;
        return this;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Boolean isIncludeFailedOnly() {
        return includeFailedOnly != null && includeFailedOnly;
    }

    public RerunEntity withIncludeFailedOnly(Boolean includeFailedOnly) {
        this.includeFailedOnly = includeFailedOnly;
        return this;
    }
}
