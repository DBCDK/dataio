package dk.dbc.dataio.jobstore.distributed.hz.serializer;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.hz.processor.UpdatePriority;

public class UpdatePrioritySer implements CompactSerializer<UpdatePriority> {
    @Override
    public UpdatePriority read(CompactReader compactReader) {
        int pri = compactReader.readInt8("p");
        boolean upgradeOnly = compactReader.readBoolean("uo");
        return new UpdatePriority(pri, upgradeOnly);
    }

    @Override
    public void write(CompactWriter compactWriter, UpdatePriority updatePriority) {
        compactWriter.writeInt8("p", (byte)updatePriority.priority);
        compactWriter.writeBoolean("uo", updatePriority.onlyIncrease);
    }

    @Override
    public String getTypeName() {
        return "update_priority";
    }

    @Override
    public Class<UpdatePriority> getCompactClass() {
        return UpdatePriority.class;
    }
}
