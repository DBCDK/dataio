package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.State;

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
    private int partNumber;
    private int numberOfChunks;
    private int numberOfItems;

    @Column(insertable = false, updatable = false)
    private Timestamp timeOfCreation;
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

    public int getId() {
        return id;
    }

    public boolean isEoj() {
        return eoj;
    }

    public void setEoj(boolean eoj) {
        this.eoj = eoj;
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
}
