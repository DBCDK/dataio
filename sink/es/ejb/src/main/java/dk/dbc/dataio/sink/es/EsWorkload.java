package dk.dbc.dataio.sink.es;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.es.ESUtil;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.List;

public class EsWorkload {
    final ExternalChunk deliveredChunk;
    final List<AddiRecord> addiRecords;
    final int userId;
    final ESUtil.PackageType packageType;
    final ESUtil.Action action;

    public EsWorkload(ExternalChunk deliveredChunk, List<AddiRecord> addiRecords,
                      int userId, ESUtil.PackageType packageType, ESUtil.Action action) {
        this.deliveredChunk = InvariantUtil.checkNotNullOrThrow(deliveredChunk, "deliveredChunk");
        this.addiRecords = InvariantUtil.checkNotNullOrThrow(addiRecords, "addiRecords");
        this.userId = userId;
        this.packageType = InvariantUtil.checkNotNullOrThrow(packageType, "packageType");
        this.action = InvariantUtil.checkNotNullOrThrow(action, "action");
    }

    public List<AddiRecord> getAddiRecords() {
        return addiRecords;
    }

    public ExternalChunk getDeliveredChunk() {
        return deliveredChunk;
    }

    public int getUserId() {
        return userId;
    }

    public ESUtil.PackageType getPackageType() {
        return packageType;
    }

    public ESUtil.Action getAction() {
        return action;
    }
}
