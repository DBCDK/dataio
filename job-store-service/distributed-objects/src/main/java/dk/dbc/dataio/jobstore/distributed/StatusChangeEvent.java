package dk.dbc.dataio.jobstore.distributed;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import dk.dbc.dataio.jobstore.distributed.hz.DataIODataSerializableFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import static dk.dbc.dataio.jobstore.distributed.hz.DataIODataSerializableFactory.Objects.STATUS_CHANGE_EVENT;

public class StatusChangeEvent implements Serializable, IdentifiedDataSerializable {
    private int sinkId;
    private ChunkSchedulingStatus oldStatus;
    private ChunkSchedulingStatus newStatus;

    public StatusChangeEvent() {
    }

    public StatusChangeEvent(int sinkId, ChunkSchedulingStatus oldStatus, ChunkSchedulingStatus newStatus) {
        this.sinkId = sinkId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public int getSinkId() {
        return sinkId;
    }

    public ChunkSchedulingStatus getOldStatus() {
        return oldStatus;
    }

    public ChunkSchedulingStatus getNewStatus() {
        return newStatus;
    }

    public void apply(Map<Integer, JobSchedulerSinkStatus> sinkStatusMap) {
        JobSchedulerSinkStatus sinkStatus = sinkStatusMap.get(sinkId);
        if(oldStatus != null) oldStatus.decSinkStatusCount(sinkStatus);
        if(newStatus != null) newStatus.incSinkStatusCount(sinkStatus);
    }

    @Override
    public int getFactoryId() {
        return DataIODataSerializableFactory.FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return STATUS_CHANGE_EVENT.ordinal();
    }

    @Override
    public void writeData(ObjectDataOutput objectDataOutput) throws IOException {
        objectDataOutput.writeInt(sinkId);
        objectDataOutput.writeObject(oldStatus);
        objectDataOutput.writeObject(newStatus);
    }

    @Override
    public void readData(ObjectDataInput objectDataInput) throws IOException {
        sinkId = objectDataInput.readInt();
        oldStatus = objectDataInput.readObject();
        newStatus = objectDataInput.readObject();
    }
}
