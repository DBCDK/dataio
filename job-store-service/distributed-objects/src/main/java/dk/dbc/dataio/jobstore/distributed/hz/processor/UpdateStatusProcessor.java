package dk.dbc.dataio.jobstore.distributed.hz.processor;

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.DependencyTracking;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.DataIODataSerializableFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static dk.dbc.dataio.jobstore.distributed.hz.DataIODataSerializableFactory.Objects.UPDATE_STATUS_PROCESSOR;

public class UpdateStatusProcessor implements EntryProcessor<TrackingKey, DependencyTracking, StatusChangeEvent>, IdentifiedDataSerializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateStatusProcessor.class);
    private ChunkSchedulingStatus schedulingStatus;

    public UpdateStatusProcessor() {
    }

    public UpdateStatusProcessor(ChunkSchedulingStatus schedulingStatus) {
        this.schedulingStatus = schedulingStatus;
    }

    @Override
    public StatusChangeEvent process(Map.Entry<TrackingKey, DependencyTracking> entry) {
        StatusChangeEvent event = new StatusChangeEvent(entry.getValue().getSinkId(), entry.getValue().getStatus(), schedulingStatus);
        entry.getValue().setStatus(schedulingStatus);
        LOGGER.debug("Status update on {} - {}", entry.getKey(), schedulingStatus);
        return event;
    }

    @Override
    public int getFactoryId() {
        return DataIODataSerializableFactory.FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return UPDATE_STATUS_PROCESSOR.ordinal();
    }

    @Override
    public void writeData(ObjectDataOutput objectDataOutput) throws IOException {
        objectDataOutput.writeInt(schedulingStatus.ordinal());
    }

    @Override
    public void readData(ObjectDataInput objectDataInput) throws IOException {
        schedulingStatus = ChunkSchedulingStatus.values()[objectDataInput.readInt()];
    }
}
