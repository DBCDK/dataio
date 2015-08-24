package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 *
 */
public class PresenterJobsImpl extends PresenterImpl {
    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterJobsImpl(ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getJobsShowView();
    }

    /**
     * Abstract Methods
     *
     *
     */
    @Override
    protected void updateBaseQuery() {
        JobListCriteriaModel model=new JobListCriteriaModel();
        model.getJobTypes().remove(JobListCriteriaModel.JobType.TEST.name());
        model.getJobTypes().remove(JobListCriteriaModel.JobType.ACCTEST.name());
        view.dataProvider.setBaseCriteria( model );
    }
}
