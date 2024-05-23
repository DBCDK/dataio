package dk.dbc.dataio.jobstore.distributed.hz.serializer;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateStatus;

public class UpdateStatusSer implements CompactSerializer<UpdateStatus> {
    @Override
    public UpdateStatus read(CompactReader compactReader) {
        Byte ex = compactReader.readNullableInt8("e");
        ChunkSchedulingStatus expected = ex == null ? null : ChunkSchedulingStatus.from(ex.intValue());
        ChunkSchedulingStatus status = ChunkSchedulingStatus.from(compactReader.readInt8("s"));
        return new UpdateStatus(expected, status);
    }

    @Override
    public void write(CompactWriter compactWriter, UpdateStatus updateStatus) {
        Byte ex = updateStatus.expectedStatus == null ? null : updateStatus.expectedStatus.value.byteValue();
        compactWriter.writeNullableInt8("e", ex);
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
