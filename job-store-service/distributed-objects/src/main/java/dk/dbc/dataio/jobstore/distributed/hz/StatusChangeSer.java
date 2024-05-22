package dk.dbc.dataio.jobstore.distributed.hz;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.StatusChangeEvent;

public class StatusChangeSer implements CompactSerializer<StatusChangeEvent> {
    @Override
    public StatusChangeEvent read(CompactReader compactReader) {
        int id = compactReader.readInt32("id");
        int os = compactReader.readInt8("os");
        int ns = compactReader.readInt8("ns");
        return new StatusChangeEvent(id, ChunkSchedulingStatus.from(os), ChunkSchedulingStatus.from(ns));
    }

    @Override
    public void write(CompactWriter compactWriter, StatusChangeEvent statusChangeEvent) {
        compactWriter.writeInt32("id", statusChangeEvent.getSinkId());
        compactWriter.writeInt8("os", statusChangeEvent.getOldStatus().value.byteValue());
        compactWriter.writeInt8("ns", statusChangeEvent.getNewStatus().value.byteValue());
    }

    @Override
    public String getTypeName() {
        return "statusChange";
    }

    @Override
    public Class<StatusChangeEvent> getCompactClass() {
        return StatusChangeEvent.class;
    }
}
