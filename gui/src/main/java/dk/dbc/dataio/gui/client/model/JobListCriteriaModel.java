package dk.dbc.dataio.gui.client.model;

public class JobListCriteriaModel extends GenericBackendModel {

    public enum JobSearchType {ALL, PROCESSING_FAILED, DELIVERING_FAILED}

    private JobSearchType jobSearchType;

    public JobListCriteriaModel() {
        this(JobSearchType.PROCESSING_FAILED); //Default value
    }

    private JobListCriteriaModel(JobSearchType searchType) {
        this.setSearchType(searchType);
    }

    public JobSearchType getSearchType() {
        return jobSearchType;
    }

    public void setSearchType(JobSearchType jobSearchType) {
        this.jobSearchType = jobSearchType;
    }


}
