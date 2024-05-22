package dk.dbc.dataio.jobstore.distributed.hz.serializer;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.TrackingKey;
import dk.dbc.dataio.jobstore.distributed.hz.processor.RemoveWaitingOn;

public class RemoveWaitingOnSer implements CompactSerializer<RemoveWaitingOn> {
    @Override
    public RemoveWaitingOn read(CompactReader compactReader) {
        TrackingKey key = new TrackingKeySer().read(compactReader);
        return new RemoveWaitingOn(key);
    }

    @Override
    public void write(CompactWriter compactWriter, RemoveWaitingOn removeWaitingOn) {
        new TrackingKeySer().write(compactWriter, removeWaitingOn.key);
    }

    @Override
    public String getTypeName() {
        return "rmWo";
    }

    @Override
    public Class<RemoveWaitingOn> getCompactClass() {
        return RemoveWaitingOn.class;
    }
}
