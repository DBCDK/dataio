package dk.dbc.dataio.sink.ticklerepo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TickleAttributes {
    @JsonProperty("submitter")
    private Integer agencyId;
    @JsonProperty
    private String datasetName;
    @JsonProperty
    private String bibliographicRecordId;
    @JsonProperty
    private String compareRecord;
    @JsonProperty
    private Boolean deleted;

    public Integer getAgencyId() {
        return agencyId;
    }

    public TickleAttributes withAgencyId(Integer agencyId) {
        this.agencyId = agencyId;
        return this;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public TickleAttributes withDatasetName(String datasetName) {
        this.datasetName = datasetName;
        return this;
    }

    public String getCompareRecord() {
        return compareRecord;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public TickleAttributes withBibliographicRecordId(String bibliographicRecordId) {
        this.bibliographicRecordId = bibliographicRecordId;
        return this;
    }

    public TickleAttributes withCompareRecord(String compareRecord) {
        this.compareRecord = compareRecord;
        return this;
    }

    public Boolean isDeleted() {
        return deleted != null && deleted;
    }

    public TickleAttributes withDeleted(Boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    @JsonIgnore
    public boolean isValid() {
        return agencyId != null
                && datasetName != null && !datasetName.trim().isEmpty()
                && bibliographicRecordId != null && !bibliographicRecordId.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "TickleAttributes{" +
                "agencyId=" + agencyId +
                ", datasetName='" + datasetName + '\'' +
                ", bibliographicRecordId='" + bibliographicRecordId + '\'' +
                ", deleted=" + deleted +
                '}';
    }
}
