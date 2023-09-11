package dk.dbc.dataio.commons.conversion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.invariant.InvariantUtil;

import java.util.Objects;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversionMetadata {
    private final String origin;
    private final String category = "dataout";
    private final boolean claimed = false;

    public ConversionMetadata(String origin) {
        this.origin = InvariantUtil.checkNotNullNotEmptyOrThrow(origin, "origin");
    }

    @JsonProperty("name")
    private String filename;

    @JsonProperty("agency")
    private Integer agencyId;

    private Integer jobId;

    public String getOrigin() {
        return origin;
    }

    public String getCategory() {
        return category;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public String getFilename() {
        return filename;
    }

    public ConversionMetadata withFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public Integer getAgencyId() {
        return agencyId;
    }

    public ConversionMetadata withAgencyId(Integer agencyId) {
        this.agencyId = agencyId;
        return this;
    }

    public Integer getJobId() {
        return jobId;
    }

    public ConversionMetadata withJobId(Integer jobId) {
        this.jobId = jobId;
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
        ConversionMetadata that = (ConversionMetadata) o;
        return claimed == that.claimed &&
                Objects.equals(origin, that.origin) &&
                Objects.equals(category, that.category) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(agencyId, that.agencyId) &&
                Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, category, claimed, filename, agencyId, jobId);
    }

    @Override
    public String toString() {
        return "ConversionMetadata{" +
                "origin='" + origin + '\'' +
                ", category='" + category + '\'' +
                ", claimed=" + claimed +
                ", filename='" + filename + '\'' +
                ", agencyId=" + agencyId +
                ", jobId=" + jobId +
                '}';
    }
}
