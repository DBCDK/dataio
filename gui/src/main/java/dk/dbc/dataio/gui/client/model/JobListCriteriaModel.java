package dk.dbc.dataio.gui.client.model;

import java.util.HashSet;
import java.util.Set;

public class JobListCriteriaModel extends GenericBackendModel {

    public enum JobSearchType {ALL, PROCESSING_FAILED, DELIVERING_FAILED}
    public enum JobType {TEST, TRANSIENT, PERSISTENT, ACCTEST}

    private JobSearchType jobSearchType;
    private String sinkId;
    private Set<String> jobTypes = new HashSet<String>();
    private String submitter;

    public JobListCriteriaModel() {
        this(JobSearchType.PROCESSING_FAILED, "0"); //Default values
    }

    private JobListCriteriaModel(JobSearchType searchType, String sinkId) {
        this.setSearchType(searchType);
        this.setSinkId(sinkId);
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

    public String getSubmitter() {
        return submitter;
    }

    public void setSubmitter(String submitter) {
        this.submitter = submitter;
    }

    /**
     * Merges the current model with the one supplied as a parameter in the call, using AND logic
     * @param model The model to merge with the current one using AND logic
     * @return The resulting model (also stored in this instance)
     */
    public JobListCriteriaModel and(JobListCriteriaModel model) {
        if (model != null) {
            setSearchType(model.getSearchType());  // The old SearchType is disgarded, and the new SearchType is used instead
            setSinkId(model.getSinkId());  // The old SinkId is disgarded, and the new SinkId is used instead
            if( model.getSubmitter() != null ) setSubmitter( model.getSubmitter());
            Set<String> oldJobTypes = new HashSet<String>(this.jobTypes.size());
            oldJobTypes.addAll(this.jobTypes);
            this.jobTypes.clear();
            for (String jobType: oldJobTypes) {
                if (model.jobTypes.contains(jobType)) {  // Only if the old AND the new supplied jobtype is present in the model
                    this.jobTypes.add(jobType);
                }
            }
        }
        return this;
    }
}
