package dk.dbc.dataio.gui.client.pages.newJob.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
* This class represents the show jobs presenter implementation
*/
public class PresenterImpl extends AbstractActivity implements Presenter {
    private ClientFactory clientFactory;
    private View view;
    private JobStoreProxyAsync jobStoreProxy;
    private PlaceController placeController;

    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterImpl(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
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
        view = clientFactory.getNewJobsShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchJobs();
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
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(model.getJobId(), model.getSubmitterName(), model.getSinkName()));
    }


    /*
     * Local methods
     */

    /**
     * This method fetches all jobs, and sends them to the view
     */
    private void fetchJobs() {
        jobStoreProxy.listJobs(new JobListCriteriaModel(), new FetchJobsCallback());
    }


    /*
     * Private classes
     */

    /**
     * This class is the callback class for the findAllJobsNew method in the Job Store
     */
    protected class FetchJobsCallback extends FilteredAsyncCallback<List<JobModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(List<JobModel> models) {
            view.setJobs(models);
        }
    }

}
