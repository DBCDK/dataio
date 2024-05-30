package dk.dbc.dataio.jobstore.distributed.hz.serializer;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateStatus;

public class UpdateStatusSer implements CompactSerializer<UpdateStatus> {
    @Override
    public UpdateStatus read(CompactReader compactReader) {
        boolean validate = compactReader.readBoolean("v");
        ChunkSchedulingStatus status = ChunkSchedulingStatus.from(compactReader.readInt8("s"));
        return new UpdateStatus(status, validate);
    }

    @Override
    public void write(CompactWriter compactWriter, UpdateStatus updateStatus) {
        compactWriter.writeBoolean("v", updateStatus.validateUpdate);
        compactWriter.writeInt8("s", updateStatus.schedulingStatus.value.byteValue());
    }

    @Override
    public String getTypeName() {
        return "upStat";
    }

    @Override
    public Class<UpdateStatus> getCompactClass() {
        return UpdateStatus.class;
    }
}
