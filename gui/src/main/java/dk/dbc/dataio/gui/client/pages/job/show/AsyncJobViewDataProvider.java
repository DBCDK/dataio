package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.List;

/**
 * Created by ja7 on 21-08-15.
 */
public class AsyncJobViewDataProvider extends AsyncDataProvider<JobModel> {

    private JobStoreProxyAsync jobStoreProxy;
    private View view;
    // The 3 Radio Buttons
    JobListCriteria userCriteria = null;
    // The selection  from the left side
    JobListCriteria baseCriteria = null;

    private int criteriaIncarnation=0;
    private JobListCriteria currentCriteriaAsJobListCriteria;

    public AsyncJobViewDataProvider(ClientFactory clientFactory, View view_ ) {
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        view = view_;

        updateCurrentCriteria();
    }

    void setBaseCriteria( JobListCriteria newBaseCriteria) {
        baseCriteria = newBaseCriteria;
        updateCurrentCriteria();
    }


    void updateCurrentCriteria() {
        criteriaIncarnation++;

        currentCriteriaAsJobListCriteria=new JobListCriteria();


        if( baseCriteria != null) {
            currentCriteriaAsJobListCriteria.and(baseCriteria);
        }

        if (userCriteria != null) {
            currentCriteriaAsJobListCriteria.where(userCriteria);
        }

        refresh();

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
            userCriteria = view.jobFilter.getValue();
            if (view.processingFailedJobsButton.getValue()) {
                userCriteria.where(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED));

            } else if (view.deliveringFailedJobsButton.getValue()) {
                userCriteria.where(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED));

            } else if (view.fatalJobsButton.getValue()) {
                userCriteria.where(new ListFilter<>(JobListCriteria.Field.WITH_FATAL_ERROR));
            } else {
                // Do notthing. implicit hit All jobs.
            }
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

        currentCriteriaAsJobListCriteria.limit(range.getLength());
        currentCriteriaAsJobListCriteria.offset(range.getStart());


        jobStoreProxy.listJobs(currentCriteriaAsJobListCriteria, new FilteredAsyncCallback<List<JobModel>>() {
                    // protection against old calls updating the view with old data.
                    int criteriaIncarnationOnRequestCall=criteriaIncarnation;
                    int offsetOnRequestCall = currentCriteriaAsJobListCriteria.getOffset();

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
                        return criteriaIncarnationOnRequestCall == criteriaIncarnation &&
                                offsetOnRequestCall == currentCriteriaAsJobListCriteria.getOffset();
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
        jobStoreProxy.countJobs(currentCriteriaAsJobListCriteria, new FilteredAsyncCallback<Long>() {
            // protection against old calls updating the view with old data.
            int criteriaIncarnationOnCall=criteriaIncarnation;

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
                return criteriaIncarnationOnCall == criteriaIncarnation;
            }

        });
    }

}
