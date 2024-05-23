package dk.dbc.dataio.jobstore.distributed.hz.serializer;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;

public class TrackingKeySer implements CompactSerializer<TrackingKey> {
    @Override
    public TrackingKey read(CompactReader compactReader) {
        int jobId = compactReader.readInt32("j");
        int chunkId = compactReader.readInt32("c");
        return new TrackingKey(jobId, chunkId);
    }

    @Override
    public void write(CompactWriter compactWriter, TrackingKey trackingKey) {
        compactWriter.writeInt32("j", trackingKey.getJobId());
        compactWriter.writeInt32("c", trackingKey.getChunkId());
    }

    @Override
    public String getTypeName() {
        return "trKey";
    }

    @Override
    public Class<TrackingKey> getCompactClass() {
        return TrackingKey.class;
    }
}
