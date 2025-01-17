package dk.dbc.dataio.jobstore.distributed.hz.serializer;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import dk.dbc.dataio.jobstore.distributed.WaitFor;

public class WaitForSer implements CompactSerializer<WaitFor> {
    @Override
    public WaitFor read(CompactReader compactReader) {
        int sinkId = compactReader.readInt32("sid");
        int submitter = compactReader.readInt32("s");
        String matchKey = compactReader.readString("mk");
        return new WaitFor(sinkId, submitter, matchKey);
    }

    @Override
    public void write(CompactWriter compactWriter, WaitFor waitFor) {
        compactWriter.writeInt32("sid", waitFor.sinkId());
        compactWriter.writeInt32("s", waitFor.submitter());
        compactWriter.writeString("mk", waitFor.matchKey());
    }

    @Override
    public String getTypeName() {
        return "waitFor";
    }

    @Override
    public Class<WaitFor> getCompactClass() {
        return WaitFor.class;
    }
}
