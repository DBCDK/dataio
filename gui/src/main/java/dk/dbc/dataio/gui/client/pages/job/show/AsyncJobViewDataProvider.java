package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;

/**
 * Created by ja7 on 21-08-15.
 */
public class AsyncJobViewDataProvider extends AsyncDataProvider<JobModel> {

    private JobStoreProxyAsync jobStoreProxy;
    private View view;
    JobListCriteriaModel userCriteria;

    JobListCriteriaModel baseCriteria;

    JobListCriteriaModel currentCriteria;

    public AsyncJobViewDataProvider(ClientFactory clientFactory, View view_ ) {
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        view = view_;
        baseCriteria = new JobListCriteriaModel();
        userCriteria = new JobListCriteriaModel();

        userCriteria.setSearchType(JobListCriteriaModel.JobSearchType.ALL);

        updateCurrentCriteria();
    }

    void setBaseCriteria( JobListCriteriaModel newBaseCriteria) {
        if( baseCriteria.equals( newBaseCriteria) ) return;
        baseCriteria = newBaseCriteria;
        updateCurrentCriteria();
    }


    void updateCurrentCriteria() {
        JobListCriteriaModel newcurrentCriteria = new JobListCriteriaModel();
        newcurrentCriteria.and(userCriteria);


        newcurrentCriteria.setJobTypes(baseCriteria.getJobTypes());

        if( !newcurrentCriteria.equals( currentCriteria )) {
            currentCriteria = newcurrentCriteria;
            refresh();
        }

    }

    void refresh( ) {
        view.refreshJobsTable();
    }

    /**
     * Call this when the jobFilter Changes values..
     *
     *
     */
    void updateUserCriteria( ) {
        if (view.selectionModel.getSelectedObject() == null) {
            final JobListCriteriaModel jobListCriteriaModel = view.jobFilter.getValue();
            if (view.processingFailedJobsButton.getValue()) {
                jobListCriteriaModel.setSearchType(JobListCriteriaModel.JobSearchType.PROCESSING_FAILED);
            } else if (view.deliveringFailedJobsButton.getValue()) {
                jobListCriteriaModel.setSearchType(JobListCriteriaModel.JobSearchType.DELIVERING_FAILED);
            } else if (view.fatalJobsButton.getValue()) {
                jobListCriteriaModel.setSearchType(JobListCriteriaModel.JobSearchType.FATAL);
            } else {
                jobListCriteriaModel.setSearchType(JobListCriteriaModel.JobSearchType.ALL);
            }
            userCriteria = jobListCriteriaModel;
        }
        updateCurrentCriteria();
    }


    /**
     * The Worker function of tha Async Data Provider.
     *
     *
     * @param display Display to get the VisibleRange from
     *
     *
     */
    @Override
    protected void onRangeChanged(final HasData<JobModel> display) {
        // Get the new range.
        final Range range = display.getVisibleRange();

        currentCriteria.setLimit(range.getLength());
        currentCriteria.setOffset(range.getStart());

        jobStoreProxy.listJobs(currentCriteria, new FilteredAsyncCallback<List<JobModel>>() {
                    // protection against old calls updating the view with old data.
                    JobListCriteriaModel criteriaOnRequestCall = currentCriteria;
                    int offsetOnRequestCall = currentCriteria.getOffset();

                    @Override
                    public void onSuccess(List<JobModel> jobModels) {
                        if( dataIsStillValid() )
                        updateRowData(range.getStart(), jobModels);
                    }

                    @Override
                    public void onFilteredFailure(Throwable e) {
                        view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
                    }


                    private boolean dataIsStillValid() {
                        return criteriaOnRequestCall.equals(currentCriteria) &&
                                offsetOnRequestCall == criteriaOnRequestCall.getOffset();
                    }

                }
        );
        updateCount();
    }

    /**
     *  Fetch a new count..
     *
     */
    public void updateCount()  {
        jobStoreProxy.countJobs(currentCriteria, new FilteredAsyncCallback<Long>() {
            // protection against old calls updating the view with old data.
            JobListCriteriaModel criteriaOnRequestCall = currentCriteria;
            @Override
            public void onSuccess(Long count) {
                if (dataIsStillValid()) {
                    updateRowCount(count.intValue(), true);
                }
            }

            @Override
            public void onFilteredFailure(Throwable e) {
                view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
            }

            private boolean dataIsStillValid() {
                return criteriaOnRequestCall.equals(currentCriteria);
            }

        });
    }

}
