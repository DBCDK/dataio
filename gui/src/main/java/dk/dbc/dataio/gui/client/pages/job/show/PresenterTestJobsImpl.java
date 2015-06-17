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
     * @param model The prepared JobListCriteriaModel data structure to pre-define the search
     */
    @Override
    protected void fetchJobsFromJobStore(JobListCriteriaModel model) {
        // When the test job type has been implemented in JobListCriteriaModel, do modify model here - before calling jobStoreProxy
        jobStoreProxy.listJobs(model, new FetchJobsCallback());
    }
}
