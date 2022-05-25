package dk.dbc.dataio.harvester.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.invariant.InvariantUtil;

import java.util.List;

public class HarvestRecordsRequest extends HarvestRequest<HarvestRecordsRequest> {
    private static final long serialVersionUID = 5138529001705123811L;

    private final List<AddiMetaData> records;
    private Integer basedOnJob;

    @JsonCreator
    public HarvestRecordsRequest(@JsonProperty("records") List<AddiMetaData> records) throws NullPointerException {
        this.records = InvariantUtil.checkNotNullOrThrow(records, "records");
    }

    public List<AddiMetaData> getRecords() {
        return records;
    }

    public HarvestRecordsRequest withBasedOnJob(Integer jobId) {
        basedOnJob = jobId;
        return this;
    }

    public Integer getBasedOnJob() {
        return basedOnJob;
    }
}
