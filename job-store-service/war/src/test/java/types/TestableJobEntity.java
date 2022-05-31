package types;

import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.State;

import java.sql.Timestamp;

public class TestableJobEntity extends JobEntity {

    public TestableJobEntity(Timestamp timeOfCreation,
                             State state,
                             JobSpecification jobSpecification,
                             SinkCacheEntity sinkCacheEntity,
                             FlowStoreReferences flowStoreReferences,
                             int numberOfItems,
                             int numberOfChunks) {

        this.timeOfCreation = timeOfCreation;
        this.setState(state);
        this.setSpecification(jobSpecification);
        this.setCachedSink(sinkCacheEntity);
        this.setFlowStoreReferences(flowStoreReferences);
        this.setNumberOfItems(numberOfItems);
        this.setNumberOfChunks(numberOfChunks);
    }
}

