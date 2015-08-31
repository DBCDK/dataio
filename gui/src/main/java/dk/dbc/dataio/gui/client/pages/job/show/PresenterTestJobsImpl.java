package dk.dbc.dataio.gui.client.pages.job.show;

import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

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
        JobListCriteria criteria=new JobListCriteria()
                .where(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TEST\"}"))
                .or(new ListFilter<JobListCriteria.Field>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"ACCTEST\"}"));

        // TODO this coud be simpler
        view.dataProvider.setBaseCriteria( criteria );

    }
}
