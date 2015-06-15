package dk.dbc.dataio.gui.client.model;

public class JobListCriteriaModel extends GenericBackendModel {

    public enum JobSearchType {ALL, PROCESSING_FAILED, DELIVERING_FAILED}

    private JobSearchType jobSearchType;
    private String sinkId;

    public JobListCriteriaModel() {
        this(JobSearchType.PROCESSING_FAILED, "0"); //Default values
    }

    private JobListCriteriaModel(JobSearchType searchType, String sinkId) {
        this.setSearchType(searchType);
        this.setSinkId(sinkId);
    }

    public JobSearchType getSearchType() {
        return jobSearchType;
    }

    public void setSearchType(JobSearchType jobSearchType) {
        this.jobSearchType = jobSearchType;
    }

    public String getSinkId() {
        return sinkId;
    }

    public void setSinkId (String sinkId) {
        this.sinkId = sinkId;
    }

}
