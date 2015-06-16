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
    }

    /**
     * Abstract Methods
     *
     * @param model The prepared JobListCriteriaModel data structure to pre-define the search
     */
    @Override
    protected void fetchJobsFromJobStore(JobListCriteriaModel model) {
        jobStoreProxy.listJobs(model, new FetchJobsCallback());
    }
}
