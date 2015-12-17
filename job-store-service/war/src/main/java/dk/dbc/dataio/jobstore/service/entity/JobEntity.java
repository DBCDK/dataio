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

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.State;
import dk.dbc.dataio.jobstore.types.WorkflowNote;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "job")
@NamedQueries({
    @NamedQuery(    name = JobEntity.NQ_FIND_SINK_BY_JOB_ID,
                    query = "SELECT job.cachedSink FROM JobEntity job WHERE job.id = :" + JobEntity.FIELD_JOB_ID)

})
public class JobEntity {
    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

    public static final String NQ_FIND_SINK_BY_JOB_ID = "NQ_FIND_SINK_BY_JOB_ID";
    public static final String FIELD_JOB_ID = "jobId";

    @Id
    @SequenceGenerator(
            name = "job_id_seq",
            sequenceName = "job_id_seq",
            allocationSize = 1)
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "job_id_seq")
    @Column(updatable = false)
    private int id;

    private boolean eoj;
    private boolean fatalError;
    private int partNumber;
    private int numberOfChunks;
    private int numberOfItems;

    @Column(insertable = false, updatable = false)
    protected Timestamp timeOfCreation;
    @Column(insertable = false, updatable = false)
    private Timestamp timeOfLastModification;
    private Timestamp timeOfCompletion;

    @Column(columnDefinition = "json")
    @Convert(converter = JobSpecificationConverter.class)
    private JobSpecification specification;

    @Column(columnDefinition = "json")
    @Convert(converter = StateConverter.class)
    private State state;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cachedFlow")
    private FlowCacheEntity cachedFlow;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cachedSink")
    private SinkCacheEntity cachedSink;

    @Column(columnDefinition = "json")
    @Convert(converter = FlowStoreReferencesConverter.class)
    private FlowStoreReferences flowStoreReferences;

    @Column(columnDefinition = "json")
    @Convert(converter = WorkflowNoteConverter.class)
    private WorkflowNote workflowNote;

    public JobEntity() {}

    public int getId() {
        return id;
    }

    public boolean isEoj() {
        return eoj;
    }

    public void setEoj(boolean eoj) {
        this.eoj = eoj;
    }

    public boolean hasFatalError() {
        return fatalError;
    }

    public void setFatalError(boolean fatalError) {
        this.fatalError = fatalError;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public void setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public void setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public Timestamp getTimeOfCreation() {
        return timeOfCreation;
    }

    public Timestamp getTimeOfCompletion() {
        return timeOfCompletion;
    }

    public void setTimeOfCompletion(Timestamp timeOfCompletion) {
        this.timeOfCompletion = new Timestamp(timeOfCompletion.getTime());
    }

    public Timestamp getTimeOfLastModification() {
        return timeOfLastModification;
    }

    public JobSpecification getSpecification() {
        return specification;
    }

    public void setSpecification(JobSpecification specification) {
        this.specification = specification;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public FlowStoreReferences getFlowStoreReferences() {
        return flowStoreReferences;
    }

    public void setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
        this.flowStoreReferences = flowStoreReferences;
    }

    public WorkflowNote getWorkflowNote() {
        return workflowNote;
    }

    public void setWorkflowNote(WorkflowNote workflowNote) {
        this.workflowNote = workflowNote;
    }

    public FlowCacheEntity getCachedFlow() {
        return cachedFlow;
    }

    public void setCachedFlow(FlowCacheEntity cachedFlow) {
        this.cachedFlow = cachedFlow;
    }

    public SinkCacheEntity getCachedSink() {
        return cachedSink;
    }

    public void setCachedSink(SinkCacheEntity cachedSink) {
        this.cachedSink = cachedSink;
    }

    public boolean hasFailedItems() {
        /*
        Due to bytecode manipulation being done internally in eclipselink
        we are prevented from using some java 8 constructs in our entity
        classes, since our current GlassFish version is stuck with v2.5.2.

        See https://bugs.eclipse.org/bugs/show_bug.cgi?id=429992 for further info.

        return Arrays.stream(State.Phase.values())
                .filter(phase -> state.getPhase(phase).getFailed() > 0)
                .map(phase -> true)
                .findFirst()
                .orElse(false);
        */

        for (State.Phase phase : State.Phase.values()) {
            if (state.getPhase(phase).getFailed() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFatalDiagnostics() {
        return state.fatalDiagnosticExists();
    }

    /* Package scoped constructor used for unit testing
     */
    JobEntity(int id) {
        this.id = id;
    }
}
