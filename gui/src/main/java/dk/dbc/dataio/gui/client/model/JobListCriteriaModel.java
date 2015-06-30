package dk.dbc.dataio.gui.client.model;

import java.util.HashSet;
import java.util.Set;

public class JobListCriteriaModel extends GenericBackendModel {

    public enum JobSearchType {ALL, PROCESSING_FAILED, DELIVERING_FAILED}
    public enum JobType {TEST, TRANSIENT, PERSISTENT}

    private JobSearchType jobSearchType;
    private String sinkId;
    private Set<String> jobTypes;

    public JobListCriteriaModel() {
        this(JobSearchType.PROCESSING_FAILED, "0", new HashSet<String>()); //Default values
    }

    private JobListCriteriaModel(JobSearchType searchType, String sinkId, Set<String> jobTypes) {
        this.setSearchType(searchType);
        this.setSinkId(sinkId);
        this.jobTypes = jobTypes;
        for (JobType jobType : JobType.values()) {
            jobTypes.add(jobType.name());
        }
    }

    public JobSearchType getSearchType() {
        return jobSearchType;
    }

    public void setSearchType(JobSearchType jobSearchType) {
        this.jobSearchType = jobSearchType;
    }

    public Set<String> getJobTypes() {
        return jobTypes;
    }

    public String getSinkId() {
        return sinkId;
    }

    public void setSinkId (String sinkId) {
        this.sinkId = sinkId;
    }

}
