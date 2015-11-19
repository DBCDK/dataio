package types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferencesBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.State;

import java.sql.Timestamp;
import java.util.Date;

import static org.mockito.Mockito.mock;

public class TestableJobEntityBuilder {

    private JobSpecification jobSpecification = new JobSpecificationBuilder().build();
    private State state = new State();
    private Timestamp timeOfCreation = new Timestamp(new Date().getTime());
    private SinkCacheEntity sinkCacheEntity = mock(SinkCacheEntity.class);
    private FlowStoreReferences flowStoreReferences = new FlowStoreReferencesBuilder().build();
    private int numberOfItems = 0;
    private int numberOfChunks = 0;

    public TestableJobEntityBuilder setJobSpecification(JobSpecification jobSpecification) {
        this.jobSpecification = jobSpecification;
        return this;
    }

    public TestableJobEntityBuilder setState(State state) {
        this.state = state;
        return this;
    }

    public TestableJobEntityBuilder setTimeOfCreation(Timestamp timeOfCreation) {
        this.timeOfCreation = timeOfCreation;
        return this;
    }

    public TestableJobEntityBuilder setSinkCacheEntity(SinkCacheEntity sinkCacheEntity) {
        this.sinkCacheEntity = sinkCacheEntity;
        return this;
    }

    public TestableJobEntityBuilder setFlowStoreReferences(FlowStoreReferences flowStoreReferences) {
        this.flowStoreReferences = flowStoreReferences;
        return this;
    }

    public TestableJobEntityBuilder setNumberOfItems(int numberOfItems) {
        this.numberOfItems = numberOfItems;
        return this;
    }

    public TestableJobEntityBuilder setNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
        return this;
    }

    public TestableJobEntity build() {
        return new TestableJobEntity(timeOfCreation, state, jobSpecification, sinkCacheEntity, flowStoreReferences, numberOfItems, numberOfChunks);
    }
}
