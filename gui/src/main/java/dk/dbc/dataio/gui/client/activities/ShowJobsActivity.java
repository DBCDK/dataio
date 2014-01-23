package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.presenters.JobsShowPresenter;
import dk.dbc.dataio.gui.client.views.JobsShowView;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * This class represents the show jobs activity
 */
public class ShowJobsActivity extends AbstractActivity implements JobsShowPresenter {
    private ClientFactory clientFactory;
    private JobsShowView jobsShowView;
//    private JobStoreProxyAsync jobStoreProxy;

    public ShowJobsActivity(/*JobsShowPlace place,*/ ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
//        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
    }

    @Override
    public void bind() {
        jobsShowView = clientFactory.getJobsShowView();
        jobsShowView.setPresenter(this);
    }

    @Override
    public void reload() {
		jobsShowView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(jobsShowView.asWidget());
        fetchJobs();
    }


    // Local methods
    private void fetchJobs() {
//        jobStoreProxy.findAllJobs(new FilteredAsyncCallback<List<JobInfo>>() {
//            @Override
//            public void onFilteredFailure(Throwable e) {
//                jobsShowView.onFailure(e.getClass().getName() + " - " + e.getMessage());
//            }
//            @Override
//            public void onSuccess(List<JobInfo> jobs) {
//                jobsShowView.setJobs(jobs);
//            }
//        });
        JobSpecification jobSpecification = new JobSpecification("Packaging", "Format", "Charset", "Destination", 1234567, "MailForNotificationAboutVerification", "MailForNotificationAboutProcessing", "ResultMailInitials", "DataFile");
        List<JobInfo> jobs = new ArrayList<JobInfo>();
        jobs.add(new JobInfo(1234567, jobSpecification, new Date(), JobErrorCode.NO_ERROR, 11, "JobResultDataCode1"));
        jobs.add(new JobInfo(2345678, jobSpecification, new Date(), JobErrorCode.NO_ERROR, 22, "JobResultDataCode2"));
        jobs.add(new JobInfo(3456789, jobSpecification, new Date(), JobErrorCode.NO_ERROR, 33, "JobResultDataCode3"));
        jobsShowView.setJobs(jobs);
    }

}
