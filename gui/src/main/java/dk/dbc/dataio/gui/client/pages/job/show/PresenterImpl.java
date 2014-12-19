package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.pages.faileditems.ShowPlace;
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
    private final PlaceController placeController;
    String jobStoreFilesystemUrl = "";  // This string should have been private, but for testing purposes, it is made package-private


    /**
     * Default constructor
     *
     * @param clientFactory The client factory to be used
     */
    public PresenterImpl(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
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
        view = clientFactory.getJobsShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        fetchJobStoreFilesystemUrl();
        fetchJobs();
    }


    /**
     * This method is called to go to the Failed Items list for the specified job
     *
     * @param jobId            The identifier for the job
     * @param operationalState The Operational State for the job
     * @param completionState  The Completion State for the job
     */
    @Override
    public void showFailedItems(String jobId, JobState.OperationalState operationalState, ItemCompletionState.State completionState) {
        placeController.goTo(new ShowPlace(jobId, operationalState, completionState));
    }

    /**
     * This method is called to go to the Failed Items list for the specified job
     *
     * @param jobId The identifier for the job
     */
    @Override
    public void showMoreInformation(String jobId) {
        Window.open(getJobstoreLink(jobId), "_blank", "");
    }



    /*
     * Local methods
     */

    /**
     * This method gets the link to the Job Store for a given id
     * This method should have been private, but for testing purposes, it is package-private
     *
     * @param id The identification of the data
     * @return The link (as a string url) to the given Job Store
     */
     String getJobstoreLink(String id) {
        return jobStoreFilesystemUrl.concat("/").concat(id);
    }

    /**
     * This method fetches all jobs, and sends them to the view
     */
    private void fetchJobs() {
        jobStoreProxy.findAllJobsNew(new FetchJobsCallback());
    }

    /**
     * This method fetches the File System URL for the Job Store, and sends it the the view
     */
    private void fetchJobStoreFilesystemUrl() {
        jobStoreProxy.getJobStoreFilesystemUrl(new GetJobstoreLinkCallback());
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

    /**
     * This class is the callback class for the getJobStoreFilesystemUrl method in the Job Store
     */
    protected class GetJobstoreLinkCallback extends FilteredAsyncCallback<String> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(String result) {
            jobStoreFilesystemUrl = result;
        }
    }
}
