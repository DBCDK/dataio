package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.rawrepo.dto.RecordIdDTO;

public class RawRepoRecordHarvestTask {
    private RecordIdDTO recordId;
    private AddiMetaData addiMetaData;
    private boolean forceAdd;

    public RecordIdDTO getRecordId() {
        return recordId;
    }

    public RawRepoRecordHarvestTask withRecordId(RecordIdDTO recordId) {
        this.recordId = recordId;
        this.forceAdd = false;
        return this;
    }

    public AddiMetaData getAddiMetaData() {
        return addiMetaData;
    }

    public RawRepoRecordHarvestTask withAddiMetaData(AddiMetaData addiMetaData) {
        this.addiMetaData = addiMetaData;
        return this;
    }

    public boolean isForceAdd() {
        return forceAdd;
    }

    public RawRepoRecordHarvestTask withForceAdd(boolean forceAdd) {
        this.forceAdd = forceAdd;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RawRepoRecordHarvestTask that = (RawRepoRecordHarvestTask) o;

        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) {
            return false;
        }
        return addiMetaData != null ? addiMetaData.equals(that.addiMetaData) : that.addiMetaData == null;
    }

    @Override
    public int hashCode() {
        int result = recordId != null ? recordId.hashCode() : 0;
        result = 31 * result + (addiMetaData != null ? addiMetaData.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RawRepoRecordHarvestTask{" +
                "recordId=" + recordId +
                ", addiMetaData=" + addiMetaData +
                '}';
    }
}
