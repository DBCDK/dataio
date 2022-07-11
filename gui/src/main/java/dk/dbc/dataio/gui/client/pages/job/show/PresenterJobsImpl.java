package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.place.shared.PlaceController;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

/**
 *
 */
public class PresenterJobsImpl extends PresenterImpl {
    /**
     * Default constructor
     *
     * @param placeController PlaceController for navigation
     * @param globalJobsView  Global Jobs View, necessary for keeping filter state etc.
     * @param header          Breadcrumb header text
     */
    public PresenterJobsImpl(PlaceController placeController, View globalJobsView, String header) {
        super(placeController, globalJobsView, header);
    }

    /**
     * Abstract Methods
     */
    @Override
    protected void updateBaseQuery() {
        JobListCriteria criteria = new JobListCriteria()
                .where(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"TRANSIENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"SUPER_TRANSIENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"PERSISTENT\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"INFOMEDIA\"}"))
                .or(new ListFilter<>(JobListCriteria.Field.SPECIFICATION, ListFilter.Op.JSON_LEFT_CONTAINS, "{ \"type\": \"COMPACTED\"}"));
        view.dataProvider.setBaseCriteria(criteria);
    }
}
