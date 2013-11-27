package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ChunkResult;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.List;

public class EsWorkload {
    final ChunkResult chunkResult;
    final List<AddiRecord> addiRecords;

    public EsWorkload(ChunkResult chunkResult, List<AddiRecord> addiRecords) {
        this.chunkResult = InvariantUtil.checkNotNullOrThrow(chunkResult, "chunkResult");
        this.addiRecords = InvariantUtil.checkNotNullOrThrow(addiRecords, "addiRecords");
    }

    public List<AddiRecord> getAddiRecords() {
        return addiRecords;
    }

    public ChunkResult getChunkResult() {
        return chunkResult;
    }
}
