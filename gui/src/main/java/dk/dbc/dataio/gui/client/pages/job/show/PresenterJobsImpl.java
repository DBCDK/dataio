package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

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
        JobListCriteria criteria=new JobListCriteria()
                .where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                .or(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"));
        view.dataProvider.setBaseCriteria( criteria );
    }
}
