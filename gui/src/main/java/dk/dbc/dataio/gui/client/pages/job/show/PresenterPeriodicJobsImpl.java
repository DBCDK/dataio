package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.place.shared.PlaceController;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

public class PresenterPeriodicJobsImpl extends PresenterImpl {
    /**
     * Default constructor
     *
     * @param placeController PlaceController for navigation
     * @param globalJobsView  Global Jobs View, necessary for keeping filter state etc.
     * @param header          Breadcrumb header text
     */
    public PresenterPeriodicJobsImpl(PlaceController placeController, View globalJobsView, String header) {
        super(placeController, globalJobsView, header);
    }

    @Override
    protected void updateBaseQuery() {
        final JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION,
                        ListFilter.Op.JSON_LEFT_CONTAINS, "{\"type\": \"PERIODIC\"}"));
        view.dataProvider.setBaseCriteria(criteria);
    }
}
