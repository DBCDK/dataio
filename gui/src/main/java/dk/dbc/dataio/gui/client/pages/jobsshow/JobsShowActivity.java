package dk.dbc.dataio.gui.client.pages.jobsshow;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
 * This class represents the show jobs activity
 */
public class JobsShowActivity extends AbstractActivity implements JobsShowGenericPresenter {
    private ClientFactory clientFactory;
    private JobsShowGenericView jobsShowView;
    private JobStoreProxyAsync jobStoreProxy;

    public JobsShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
    }

    @Override
    public void bind() {
        jobsShowView = clientFactory.getJobsShowView();
        jobsShowView.setPresenter(this);
    }

    @Override
    public void reload() {
        fetchJobs();
		jobsShowView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(jobsShowView.asWidget());
        jobsShowView.clearFields();
        fetchJobs();
    }


    // Local methods
    private void fetchJobs() {
        jobStoreProxy.findAllJobs(new FilteredAsyncCallback<List<JobInfo>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                jobsShowView.onFailure(e.getClass().getName() + " - " + e.getMessage());
            }
            @Override
            public void onSuccess(List<JobInfo> jobs) {
                jobsShowView.setJobs(jobs);
            }
        });
    }

}
