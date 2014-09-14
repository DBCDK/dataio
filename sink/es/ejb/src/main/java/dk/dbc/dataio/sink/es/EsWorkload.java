package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.SinkChunkResult;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.List;

public class EsWorkload {
    final SinkChunkResult sinkChunkResult;
    final List<AddiRecord> addiRecords;

    public EsWorkload(SinkChunkResult sinkChunkResult, List<AddiRecord> addiRecords) {
        this.sinkChunkResult = InvariantUtil.checkNotNullOrThrow(sinkChunkResult, "sinkChunkResult");
        this.addiRecords = InvariantUtil.checkNotNullOrThrow(addiRecords, "addiRecords");
    }

    public List<AddiRecord> getAddiRecords() {
        return addiRecords;
    }

    public SinkChunkResult getSinkChunkResult() {
        return sinkChunkResult;
    }
}
