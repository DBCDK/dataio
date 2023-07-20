package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
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
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Optional;

@Entity
@Table(name = "job")
public class JobEntity {
    /* Be advised that updating the internal state of a 'json' column
       will not mark the field as dirty and therefore not result in a
       database update. The only way to achieve an update is to replace
       the field value with a new instance (long live copy constructors).
     */

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
    private int priority;
    private int skipped;

    // TODO: 4/4/17 Drop timeOfLastModification db trigger and use @PrePersist and @PreUpdate callbacks instead (to avoid unnecessary flush() and refresh() calls)

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

    public JobEntity() {
    }

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

    public Priority getPriority() {
        return Priority.of(priority);
    }

    public void setPriority(Priority priority) {
        if (priority != null) {
            this.priority = priority.getValue();
        }
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

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    /**
     * pt. it's only used by the Tickle Repo JobScheduling code for Correct TerminationChunkScheduling
     *
     * @return DatasetId for the job.
     */
    public long lookupDataSetId() {
        return getSpecification().getSubmitterId();

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

    /* constructor used for testing */
    public JobEntity(int id) {
        this.id = id;
    }

    public String getProcessorQueue() {
        return Optional.ofNullable(getSpecification()).map(JobSpecification::getType).map(t -> t.processorQueue).orElse(null);
    }

    public String getSinkQueue() {
        return Optional.ofNullable(getCachedSink()).map(SinkCacheEntity::getSink).map(Sink::getContent).map(SinkContent::getQueue).orElse(null);
    }
}
