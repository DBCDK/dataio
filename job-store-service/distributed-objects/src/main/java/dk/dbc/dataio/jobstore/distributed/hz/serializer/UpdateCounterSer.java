package dk.dbc.dataio.jobstore.distributed.hz.serializer;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.ChunkSchedulingStatus;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdateCounter;

import java.util.EnumMap;
import java.util.Map;

public class UpdateCounterSer implements CompactSerializer<UpdateCounter> {
    @Override
    public UpdateCounter read(CompactReader compactReader) {
        EnumMap<ChunkSchedulingStatus, Integer> map = new EnumMap<>(ChunkSchedulingStatus.class);
        byte size = compactReader.readInt8("s");
        for (int i = 0; i < size; i++) {
            ChunkSchedulingStatus key = ChunkSchedulingStatus.from(compactReader.readInt8("k"));
            int value = compactReader.readInt32("v");
            map.put(key, value);
        }
        return new UpdateCounter(map);
    }

    @Override
    public void write(CompactWriter compactWriter, UpdateCounter updateCounter) {
        Map<ChunkSchedulingStatus, Integer> deltas = updateCounter.deltas;
        compactWriter.writeInt8("s", (byte) deltas.size());
        for (Map.Entry<ChunkSchedulingStatus, Integer> entry : deltas.entrySet()) {
            compactWriter.writeInt8("k", entry.getKey().value.byteValue());
            compactWriter.writeInt32("v", entry.getValue());
        }
    }

    @Override
    public String getTypeName() {
        return "upCnt";
    }

    @Override
    public Class<UpdateCounter> getCompactClass() {
        return UpdateCounter.class;
    }
}
