package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 */
public class PresenterTestJobsImpl extends PresenterImpl {
    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterTestJobsImpl(ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getTestJobsShowView();
    }

    /**
     * Abstract Methods
     *
     *
     */
    @Override
    protected void updateBaseQuery() {
        JobListCriteriaModel model=new JobListCriteriaModel();
        model.getJobTypes().remove(JobListCriteriaModel.JobType.TRANSIENT.name());
        model.getJobTypes().remove(JobListCriteriaModel.JobType.PERSISTENT.name());
        view.dataProvider.setBaseCriteria( model );

    }


}
