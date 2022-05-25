package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AsyncJobViewDataProvider extends AsyncDataProvider<JobModel> {
    private static final Logger logger = Logger.getLogger(FilteredAsyncCallback.class.getName());

    private CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private boolean autoRefresh = true;

    private View view;

    // The selection  from the left side
    JobListCriteria baseCriteria = null;

    private int criteriaIncarnation = 0;
    JobListCriteria currentCriteria = new JobListCriteria();

    private List<JobModel> currentViewJobModel;
    private int currentViewListStart = -1;
    private Timer autoUpdateTimer;

    public AsyncJobViewDataProvider(View view, ProvidesKey keyProvider) {
        super(keyProvider);
        this.view = view;
    }

    void setBaseCriteria(JobListCriteria newBaseCriteria) {
        baseCriteria = newBaseCriteria;
        updateCurrentCriteria();
    }

    void updateCurrentCriteria() {
        JobListCriteria newJobListCriteria = view.jobFilter.getValue();
        if (baseCriteria != null) {
            newJobListCriteria.and(baseCriteria);
        }
        if (!currentCriteria.equals(newJobListCriteria)) {
            criteriaIncarnation++;
            currentCriteria = newJobListCriteria;
            refresh();
        }
    }

    void refresh() {
        view.loadJobsTable();
    }


    private void setNewJobModel(int start, List<JobModel> jobModels) {
        cancelAutoUpdateTimer();

        currentViewListStart = start;
        currentViewJobModel = jobModels;
        updateRowData(start, jobModels);

        autoUpdateJobModelsIfNecessary();
    }

    /**
     * Check if Any job in the currentViewModel may change.. if so, sets a timer and make a query to the job store
     * when to check for changes in the jobs. the currentViewModel
     */
    private void autoUpdateJobModelsIfNecessary() {
        // Test if model Has Unfinished Jobs
        List<String> jobIdsToUpdate = currentViewJobModel.stream()
                .filter(jobModel -> jobModel.getJobCompletionTime().isEmpty())
                .map(JobModel::getJobId).collect(Collectors.toList());

        // Unfinished jobs found:
        if (!jobIdsToUpdate.isEmpty()) {
            // create jobListCriteria
            final JobListCriteria findJobsByIds = buildJobListCriteria(jobIdsToUpdate);
            // create new timer
            configureNewAutoUpdateTimer(findJobsByIds);

        } else {
            cancelAutoUpdateTimer();
        }
    }

    private JobListCriteria buildJobListCriteria(List<String> jobIdsToUpdate) {
        JobListCriteria findJobsByIds = new JobListCriteria();
        boolean first = true;
        for (String jobId : jobIdsToUpdate) {
            if (first) {
                first = false;
                findJobsByIds.where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
            } else {
                findJobsByIds.or(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
            }
        }
        return findJobsByIds;
    }


    private void configureNewAutoUpdateTimer(JobListCriteria findJobsByIds) {
        autoUpdateTimer = new Timer() {
            int criteriaIncarnationOnRequestCall = criteriaIncarnation;
            List<JobModel> modelAtTimeOfList = currentViewJobModel;

            @Override
            public void run() {
                if (!dataIsStillValid()) return;

                commonInjector.getJobStoreProxyAsync().listJobs(findJobsByIds, new FilteredAsyncCallback<List<JobModel>>() {
                    @Override
                    public void onSuccess(List<JobModel> jobModels) {
                        if (dataIsStillValid()) {
                            logger.info("auto update query result: " + jobModels.size() + " " + jobModels.stream().map(j -> j.getJobId()).collect(Collectors.joining(",")));
                            mergeUpdatedEntries(jobModels);
                            autoUpdateJobModelsIfNecessary();
                        }
                    }

                    @Override
                    public void onFilteredFailure(Throwable e) {
                        logger.warning("auto update query failed " + e.getMessage());
                        autoUpdateJobModelsIfNecessary();
                    }
                });
            }

            private boolean dataIsStillValid() {
                return criteriaIncarnationOnRequestCall == criteriaIncarnation &&
                        modelAtTimeOfList == currentViewJobModel;
            }

        }; // End of New Timer
        autoUpdateTimer.schedule(800); // 0.8 second
    }


    /**
     * @param jobModels List of JobModels to incorporate in current list of JobModels on Screen
     */

    private void mergeUpdatedEntries(List<JobModel> jobModels) {
        if (!autoRefresh) return;  // If autorefresh is disabled, don't refresh
        if (view.hasAssigneeFieldFocus())
            return;  // If assignee field has focus, don't refresh display, since then Assignee Field focus will be lost

        Map<String, JobModel> idMap = new HashMap<>();
        for (JobModel jm : jobModels) {
            idMap.put(jm.getJobId(), jm);
        }

        for (int i = 0; i < currentViewJobModel.size(); ++i) {
            final JobModel currentItem = currentViewJobModel.get(i);
            final JobModel newItem = idMap.get(currentItem.getJobId());
            if (newItem != null && counterOrStatusUpdated(currentItem, newItem)) {
                currentViewJobModel.set(i, newItem);
                // Doing Single row updates to avoid screen flicker for jobs not changed
                updateRowData(currentViewListStart + i, Collections.singletonList(newItem));
            }
        }
    }

    /**
     * Counter or Status updated
     *
     * @param oldItem The Old Item
     * @param newItem The New Item
     * @return If relevant data is updated
     */
    private boolean counterOrStatusUpdated(JobModel oldItem, JobModel newItem) {
        return oldItem.getNumberOfItems() != newItem.getNumberOfItems() ||
                oldItem.getStateModel().getPartitionedCounter() != newItem.getStateModel().getPartitionedCounter() ||
                oldItem.getStateModel().getProcessedCounter() != newItem.getStateModel().getProcessedCounter() ||
                oldItem.getStateModel().getDeliveredCounter() != newItem.getStateModel().getDeliveredCounter() ||
                !oldItem.getJobCompletionTime().equals(newItem.getJobCompletionTime()) ||
                !newItem.getJobCompletionTime().isEmpty() ||
                !newItem.getDiagnosticModels().isEmpty();

    }

    /**
     * Safely Cancel autoUpdateTimer
     */
    private void cancelAutoUpdateTimer() {
        if (autoUpdateTimer != null) {
            autoUpdateTimer.cancel();
            autoUpdateTimer = null;
        }
    }


    /**
     * The Worker function of the Async Data Provider.
     *
     * @param display Display to get the VisibleRange from
     */
    @Override
    protected void onRangeChanged(final HasData<JobModel> display) {
        // Get the new range.
        final Range range = display.getVisibleRange();

        currentCriteria.limit(range.getLength());
        currentCriteria.offset(range.getStart());


        commonInjector.getJobStoreProxyAsync().listJobs(currentCriteria, new FilteredAsyncCallback<List<JobModel>>() {
                    // protection against old calls updating the view with old data.
                    int criteriaIncarnationOnRequestCall = criteriaIncarnation;
                    int offsetOnRequestCall = currentCriteria.getOffset();

                    @Override
                    public void onSuccess(List<JobModel> jobModels) {
                        if (dataIsStillValid()) {
                            setNewJobModel(range.getStart(), jobModels);
                        }
                    }

                    @Override
                    public void onFilteredFailure(Throwable e) {
                        view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
                    }

                    private boolean dataIsStillValid() {
                        return criteriaIncarnationOnRequestCall == criteriaIncarnation &&
                                offsetOnRequestCall == currentCriteria.getOffset();
                    }
                }
        );
        updateCount();
    }

    /**
     * Fetch a new count..
     */
    private void updateCount() {
        commonInjector.getJobStoreProxyAsync().countJobs(currentCriteria, new FilteredAsyncCallback<Long>() {
            // protection against old calls updating the view with old data.
            int criteriaIncarnationOnCall = criteriaIncarnation;

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

    /**
     * Sets whether the display is refreshed automagically
     *
     * @param autoRefresh True: Do autorefresh, False: Do not autorefresh
     */
    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }
}
