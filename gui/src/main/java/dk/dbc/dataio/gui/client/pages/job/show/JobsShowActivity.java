package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.pages.faileditems.ShowPlace;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
 * This class represents the show jobs activity
 */
public class JobsShowActivity extends AbstractActivity implements JobsShowPresenter {
    private ClientFactory clientFactory;
    private JobsShowView jobsShowView;
    private JobStoreProxyAsync jobStoreProxy;
    private final PlaceController placeController;

    public JobsShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }

    private void bind() {
        jobsShowView = clientFactory.getJobsShowView();
        jobsShowView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(jobsShowView.asWidget());
        jobsShowView.clearFields();
        fetchJobs();
        fetchJobStoreFilesystemUrl();
    }

    @Override
    public void showFailedItems(long jobId) {
        placeController.goTo(new ShowPlace(jobId));
    }

    // Local methods
    private void fetchJobs() {
        jobStoreProxy.findAllJobs(new FilteredAsyncCallback<List<JobInfo>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                jobsShowView.setErrorText(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<JobInfo> jobs) {
                jobsShowView.setJobs(jobs);
            }
        });
    }

    private void fetchJobStoreFilesystemUrl() {
        jobStoreProxy.getJobStoreFilesystemUrl(new FilteredAsyncCallback<String>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                jobsShowView.setErrorText(e.getClass().getName() + " - " + e.getMessage());
            }

            @Override
            public void onSuccess(String result) {
                jobsShowView.setJobStoreFilesystemUrl(result);
            }
        });
    }

}
