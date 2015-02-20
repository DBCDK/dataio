package dk.dbc.dataio.gui.client.model;

public class JobListCriteriaModel extends GenericBackendModel {

    private String jobId;

    public JobListCriteriaModel() {
        this.jobId = "0";
    }

    public String getJobId() {
        return jobId;
    }

}
