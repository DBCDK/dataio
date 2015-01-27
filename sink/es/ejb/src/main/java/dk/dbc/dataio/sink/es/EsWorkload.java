package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.List;

public class EsWorkload {
    final ExternalChunk deliveredChunk;
    final List<AddiRecord> addiRecords;

    public EsWorkload(ExternalChunk deliveredChunk, List<AddiRecord> addiRecords) {
        this.deliveredChunk = InvariantUtil.checkNotNullOrThrow(deliveredChunk, "deliveredChunk");
        this.addiRecords = InvariantUtil.checkNotNullOrThrow(addiRecords, "addiRecords");
    }

    public List<AddiRecord> getAddiRecords() {
        return addiRecords;
    }

    public ExternalChunk getDeliveredChunk() {
        return deliveredChunk;
    }
}
