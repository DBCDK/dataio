package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;


/**
* This class represents the show jobs presenter implementation
*/
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    protected View view;
    protected JobStoreProxyAsync jobStoreProxy;
    private PlaceController placeController;

    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterImpl(ClientFactory clientFactory) {
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        updateBaseQuery();
    }


    /*
     * Overrides
     */

    /**
     * This method is a result of a click on one job in the list, and activates the Item Show page
     * @param model The model, containing the selected item
     */
    @Override
    public void itemSelected(JobModel model) {
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(model.getJobId()));
    }

    @Override
    public void updateSelectedJobs() {
        view.selectionModel.clear();
        view.dataProvider.updateUserCriteria();
        view.dataProvider.updateCurrentCriteria();
        view.jobsTable.setVisibleRangeAndClearData(new Range(0, 20), true);
    }


    @Override
    public void refresh() {
        view.refreshJobsTable();
    }

    /**
     * Abstract Methods
     */

    protected abstract void updateBaseQuery();





}
