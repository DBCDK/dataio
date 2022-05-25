package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.components.log.LogPanel;
import dk.dbc.dataio.gui.client.components.log.LogPanelMessages;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.List;

import static dk.dbc.dataio.gui.client.views.ContentPanel.GUIID_CONTENT_PANEL;

/**
 * Concrete Presenter Implementation Class for Job Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private Long jobId;
    private Boolean failedItemsOnly;
    LogPanel logPanel;
    JobRerunScheme jobRerunScheme;


    /**
     * Constructor
     *
     * @param place  the place
     * @param header Breadcrumb header text
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        jobId = Long.valueOf(place.getParameter(EditPlace.JOB_ID));
        failedItemsOnly = Boolean.valueOf(place.getParameter(EditPlace.FAILED_ITEMS_ONLY));

        if (Document.get().getElementById(GUIID_CONTENT_PANEL) != null && Document.get().getElementById(GUIID_CONTENT_PANEL).getPropertyObject(GUIID_CONTENT_PANEL) != null) {
            logPanel = ((ContentPanel) Document.get().getElementById(GUIID_CONTENT_PANEL).getPropertyObject(GUIID_CONTENT_PANEL)).getLogPanel();
        }
    }

    /**
     * Initializing the model
     */

    @Override
    protected void initializeViewFields(JobRerunScheme jobRerunScheme) {
        final View view = getView();
        final boolean isEnableViewFields =
                jobRerunScheme.getType() == JobRerunScheme.Type.RR
                        || failedItemsOnly
                        || jobRerunScheme.getType() == JobRerunScheme.Type.TICKLE;

        // Below fields are disabled only if the job is of type raw repo or if
        // the chosen rerun includes exclusively failed items.
        view.packaging.setEnabled(!isEnableViewFields);
        view.format.setEnabled(!isEnableViewFields);
        view.charset.setEnabled(!isEnableViewFields);
        view.destination.setEnabled(!isEnableViewFields);
        view.mailForNotificationAboutVerification.setEnabled(!isEnableViewFields);
        view.mailForNotificationAboutProcessing.setEnabled(!isEnableViewFields);
        view.resultMailInitials.setEnabled(!isEnableViewFields);
        view.type.setEnabled(!isEnableViewFields);

        // Below fields are always disabled
        view.jobId.setEnabled(false);
        view.jobcreationtime.setEnabled(false);
        view.jobcompletiontime.setEnabled(false);
        view.partnumber.setEnabled(false);
        view.datafile.setEnabled(false);
    }

    @Override
    public void initializeModel() {
        getJob();
    }


    /**
     * Creates rerun task for given job
     */
    @Override
    void doReSubmitJobInJobStore() {
        if (jobRerunScheme.getActions().contains(JobRerunScheme.Action.COPY)) {
            commonInjector.getJobStoreProxyAsync().reSubmitJob(this.jobModel, new ReSubmitJobFilteredAsyncCallback());
        } else {
            commonInjector.getJobStoreProxyAsync().createJobRerun(jobId.intValue(), failedItemsOnly, new CreateJobRerunAsyncCallback());
        }
        // Go back to the job show page right away as there can potentially be waiting time before the rerun is executed.
        // Clear the log to make it visible to the user when the rerun occurs.
        logPanel.clear();
        History.back();
    }

    // Private methods
    private void getJob() {
        final JobListCriteria findJobById = new JobListCriteria().where(new ListFilter<>(JobListCriteria.Field.JOB_ID, ListFilter.Op.EQUAL, jobId));
        commonInjector.getJobStoreProxyAsync().listJobs(findJobById, new GetJobModelFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to listJobs in jobstore proxy
     */
    class GetJobModelFilteredAsyncCallback extends FilteredAsyncCallback<List<JobModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "jobId: " + jobId;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromJobStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(List<JobModel> jobModels) {
            if (jobModels != null && jobModels.size() > 0) {
                setJobModel(jobModels.get(0));
                getJobRerunScheme(jobModel);
            }
        }
    }

    private void getJobRerunScheme(JobModel jobModel) {
        commonInjector.getJobRerunProxyAsync().parse(jobModel, new GetJobRerunSchemeFilteredAsyncCallback());
    }

    class GetJobRerunSchemeFilteredAsyncCallback extends FilteredAsyncCallback<JobRerunScheme> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            callbackOnFailure(caught);
        }

        @Override
        public void onSuccess(JobRerunScheme jobRerunScheme) {
            updateAllFieldsAccordingToCurrentState(jobRerunScheme);
            setJobRerunScheme(jobRerunScheme);
        }
    }

    private void setJobRerunScheme(JobRerunScheme jobRerunScheme) {
        this.jobRerunScheme = jobRerunScheme;
    }


    /**
     * Call back class to be instantiated in the call to reSubmitJob in jobstore proxy
     */
    class ReSubmitJobFilteredAsyncCallback extends FilteredAsyncCallback<JobModel> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            callbackOnFailure(caught);
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            callbackOnSuccess(LogPanelMessages.rerunFromFileStore(jobModel.getJobId(), String.valueOf(jobId)));
        }
    }

    /**
     * Call back class to be instantiated in the call to createJobRerun in jobstore proxy (RR)
     */
    class CreateJobRerunAsyncCallback implements AsyncCallback<Void> {
        @Override
        public void onFailure(Throwable caught) {
            callbackOnFailure(caught);
        }

        @Override
        public void onSuccess(Void result) {
            callbackOnSuccess(LogPanelMessages.rerunFromJobStore(failedItemsOnly, String.valueOf(jobId)));
        }
    }

    private void callbackOnFailure(Throwable caught) {
        String msg = "jobId: " + jobId;
        getView().setErrorText(ProxyErrorTranslator.toClientErrorFromJobStoreProxy(caught, commonInjector.getProxyErrorTexts(), msg));
    }

    private void callbackOnSuccess(String logMessage) {
        logPanel.showMessage(logMessage);
    }
}
