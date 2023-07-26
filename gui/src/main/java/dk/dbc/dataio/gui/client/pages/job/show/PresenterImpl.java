package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.jobfilter.SinkJobFilter;
import dk.dbc.dataio.gui.client.components.log.LogPanel;
import dk.dbc.dataio.gui.client.components.log.LogPanelMessages;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dk.dbc.dataio.gui.client.views.ContentPanel.GUIID_CONTENT_PANEL;


/**
 * This class represents the show jobs presenter implementation
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    public final static String SHOW_EARLIEST_ACTIVE = "ShowEarliestActive";

    private static final String TRANSPARENT = "transparent";
    private static final String EMPTY = "";
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private PlaceController placeController;
    String jobId;
    private String header;
    protected View view;
    private Texts texts;
    boolean isMultipleRerun;
    LogPanel logPanel;

    public enum Background {DEFAULT, BLUE_OCEAN, BLUE_TWIRL, ROSE_PETALS}

    /**
     * Default constructor
     *
     * @param placeController PlaceController for navigation
     * @param view            Global Jobs View, necessary for keeping filter state etc.
     * @param header          Breadcrumb header text
     */
    public PresenterImpl(PlaceController placeController, View view, String header) {
        this.placeController = placeController;
        this.view = view;
        this.header = header;
        this.texts = view.getTexts();
        setupChangeColorSchemeListBox();
    }

    /*
     * Overrides
     */


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
        AbstractBasePlace place = (AbstractBasePlace) placeController.getWhere();
        view.jobFilter.setPlace(place);
        view.setPresenter(this);
        view.setHeader(this.header);
        containerWidget.setWidget(view.asWidget());
        updateBaseQuery();
        refresh();
    }

    @Override
    public void rerun() {
        if (isMultipleRerun) {
            rerunMultiple(validRerunJobsFilter(getShownJobModels()));
        } else {
            rerunSingle(view.selectionModel.getSelectedObject(), view.popupSelectBox.isRightSelected());
        }
    }

    private void executeRerun(JobModel jobModel, JobRerunScheme jobRerunScheme, boolean failedItemsOnly) {
        if (isMultipleRerun) {
            if (jobRerunScheme.getActions().contains(JobRerunScheme.Action.COPY)) {
                commonInjector.getJobStoreProxyAsync().reSubmitJob(
                        jobModel,
                        new ReSubmitJobFilteredAsyncCallback(jobModel.getJobId()));
            } else {
                commonInjector.getJobStoreProxyAsync().createJobRerun(
                        Long.valueOf(jobModel.getJobId()).intValue(),
                        failedItemsOnly,
                        new CreateJobRerunAsyncCallback(jobModel.getJobId(), failedItemsOnly));
            }
        } else {
            rerunSingle(jobModel, failedItemsOnly);
        }
    }

    /**
     * Finds all JobModels from the shown jobs in the jobs table
     *
     * @return List of JobModels
     */
    @Override
    public List<JobModel> getShownJobModels() {
        final List<JobModel> models = new ArrayList<>();
        int count = view.jobsTable.getVisibleItemCount();
        for (int i = 0; i < count; i++) {
            models.add((JobModel) view.jobsTable.getVisibleItem(i));
        }
        models.sort(Comparator.comparing(model -> Integer.valueOf(model.getJobId())));
        return models;
    }


    @Override
    public List<JobModel> validRerunJobsFilter(List<JobModel> jobModels) {
        List<JobModel> validJobModels = new ArrayList<>();
        for (JobModel jobModel : jobModels) {
            if (jobModel.getType() != JobSpecification.Type.COMPACTED) {
                validJobModels.add(jobModel);
            }
        }
        return validJobModels;
    }

    @Override
    public void setIsMultipleRerun(boolean isMultipleRerun) {
        this.isMultipleRerun = isMultipleRerun;
    }

    /**
     * This method is a result of a click on one job in the list, and activates the Item Show page
     *
     * @param model The model, containing the selected item
     */
    @Override
    public void itemSelected(JobModel model) {
        Optional<ListFilterGroup.Member<JobListCriteria.Field>> memberOptional = Optional.empty();
        final JobListCriteria value = view.jobFilter.getValue();
        if (value.getFiltering().size() > 0) {
            for (ListFilterGroup<JobListCriteria.Field> filterGroup : value.getFiltering()) {
                memberOptional = filterGroup.getMembers().stream().filter(s -> s.getFilter().getField().name().equals("RECORD_ID")).findFirst();
                if (memberOptional.isPresent()) {
                    break;
                }
            }
        }
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(model.getJobId(),
                memberOptional.map(fieldMember -> fieldMember.getFilter().getValue()).orElse(null)));
    }

    @Override
    public void updateSelectedJobs() {
        view.dataProvider.updateCurrentCriteria();
        view.refreshJobsTable();
    }


    @Override
    public void refresh() {
        view.refreshJobsTable();
    }

    /**
     * Method used to look up a specified job and display the job in the items show view
     */
    @Override
    public void showJob() {
        this.jobId = view.jobIdInputField.getValue().trim();
        if (isJobIdValid()) {
            countExistingJobsWithJobId();
        }
    }

    @Override
    public void showLog() {
        getLogPanel().showLog();
    }

    @Override
    public void clearLog() {
        getLogPanel().clear();
    }

    @Override
    public void showHistory() {
        getLogPanel().showHistory();
    }

    /**
     * Method used to set a Workflow Note for this job
     *
     * @param workflowNoteModel The workflow model to set
     * @param jobId             The jobid for the Job, to which the Workflow Note should be attached
     */
    @Override
    public void setWorkflowNote(WorkflowNoteModel workflowNoteModel, String jobId) {
        commonInjector.getJobStoreProxyAsync().setWorkflowNote(workflowNoteModel, Long.valueOf(jobId).intValue(), new SetWorkflowNoteCallBack());
    }

    @Override
    public void changeColorSchemeListBoxShow() {
        view.changeColorSchemeListBox.show();
    }

    @Override
    public void changeColorScheme(Map<String, String> colorScheme) {
        final String imageResource;
        if (colorScheme.containsKey(Background.BLUE_OCEAN.name())) {
            imageResource = "url('" + commonInjector.getResources().blue_ocean().getSafeUri().asString() + "')";
            setPanelBackgrounds(imageResource, TRANSPARENT, TRANSPARENT);
        } else if (colorScheme.containsKey(Background.BLUE_TWIRL.name())) {
            imageResource = "url('" + commonInjector.getResources().blue_twirl().getSafeUri().asString() + "')";
            setPanelBackgrounds(imageResource, TRANSPARENT, TRANSPARENT);
        } else if (colorScheme.containsKey(Background.ROSE_PETALS.name())) {
            imageResource = "url('" + commonInjector.getResources().rose_petals().getSafeUri().asString() + "')";
            setPanelBackgrounds(imageResource, TRANSPARENT, TRANSPARENT);
        } else {
            setPanelBackgrounds("none", "#f2f0ec", "#f9f9f7");
        }
    }

    /**
     * This method evaluates the assignee given as input.
     * If the assignee is empty, an error is displayed in the view.
     * Otherwise the assignee input value is set on a new workflow note model.
     *
     * @param workflowNoteModel The existing WorkflowNoteModel
     * @param assignee          the assignee to set
     * @return null if the assignee value was empty, otherwise a new workflow note model with the input string set as assignee.
     */
    @Override
    public WorkflowNoteModel preProcessAssignee(WorkflowNoteModel workflowNoteModel, String assignee) {
        WorkflowNoteModel newWorkflowNoteModel = null;
        if (assignee.trim().equals(EMPTY)) {
            view.setErrorText(texts.error_CheckboxCellValidationError());
        } else {
            newWorkflowNoteModel = mapValuesToWorkflowNoteModel(workflowNoteModel);
            newWorkflowNoteModel.setAssignee(assignee);
        }
        return newWorkflowNoteModel;
    }

    @Override
    public void getJobRerunScheme(JobModel jobModel) {
        commonInjector.getJobRerunProxyAsync().parse(jobModel, new GetJobRerunSchemeFilteredAsyncCallback(jobModel));
    }

    public void resendJob(JobModel jobModel) {
        commonInjector.getJobStoreProxyAsync().resendJob(jobModel, new ACallback<>());
    }

    @Override
    public void abortJob(JobModel jobModel) {
        commonInjector.getJobStoreProxyAsync().abortJob(jobModel, new ACallback<>());
    }

    /**
     * Re runs a series of jobs, based on the input list job models
     *
     * @param jobModels The JobModels to rerunAll
     */
    void rerunMultiple(List<JobModel> jobModels) {
        // Due to History.back() from the edit job page, we have to make sure we have the correct logPanel...
        clearLog();
        for (JobModel jobModel : jobModels) {
            commonInjector.getJobRerunProxyAsync().parse(jobModel, new GetJobRerunSchemeFilteredAsyncCallback(jobModel));
        }
    }

    private void rerunSingle(JobModel jobModel, boolean failedItemsOnly) {
        // Due to History.back() from the edit job page, we have to make sure we have the correct logPanel...
        clearLog();
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.job.modify.EditPlace(jobModel, failedItemsOnly));
    }

    /**
     * Call back class to be instantiated in the call to reSubmitJob in jobstore proxy
     */
    class ReSubmitJobFilteredAsyncCallback extends FilteredAsyncCallback<JobModel> {

        String oldJobId;

        ReSubmitJobFilteredAsyncCallback(String oldJobId) {
            this.oldJobId = oldJobId;
        }

        @Override
        public void onFilteredFailure(Throwable caught) {
            logPanel.showMessage(caught.getMessage());
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            logPanel.showMessage(LogPanelMessages.rerunFromFileStore(jobModel.getJobId(), oldJobId));
        }
    }

    /**
     * Call back class to be instantiated in the call to createJobRerun in jobstore proxy (RR)
     */
    class CreateJobRerunAsyncCallback implements AsyncCallback<Void> {
        private final String oldJobId;
        private final boolean failedItemsOnly;

        CreateJobRerunAsyncCallback(String oldJobId, boolean failedItemsOnly) {
            this.oldJobId = oldJobId;
            this.failedItemsOnly = failedItemsOnly;
        }

        @Override
        public void onFailure(Throwable caught) {
            setLogMessage(caught.getMessage());
        }

        @Override
        public void onSuccess(Void result) {
            setLogMessage(LogPanelMessages.rerunFromJobStore(failedItemsOnly, oldJobId));
        }
    }

    class GetJobRerunSchemeFilteredAsyncCallback extends FilteredAsyncCallback<JobRerunScheme> {

        private JobModel jobModel;

        GetJobRerunSchemeFilteredAsyncCallback(JobModel jobModel) {
            this.jobModel = jobModel;
        }

        @Override
        public void onFilteredFailure(Throwable caught) {
            setLogMessage(caught.getMessage());
        }

        @Override
        public void onSuccess(JobRerunScheme jobRerunScheme) {
            if (!isMultipleRerun) {
                if (jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_FAILED)) {
                    view.setPopupSelectBoxVisible();
                } else {
                    executeRerun(jobModel, jobRerunScheme, false);
                }
            } else {
                if (view.popupSelectBox.isRightSelected() && !jobRerunScheme.getActions().contains(JobRerunScheme.Action.RERUN_FAILED)) {
                    handleMultipleRerunErrorScenarios(jobModel, jobRerunScheme);
                } else {
                    executeRerun(jobModel, jobRerunScheme, view.popupSelectBox.isRightSelected());
                }
            }
        }
    }

    /**
     * Sets a new Place Token for the view
     *
     * @param place The place to use for setting up the filter
     */
    @Override
    public void setPlace(AbstractBasePlace place) {
        if (view != null && view.jobFilter != null) {
            view.jobFilter.updatePlace(place);
        }
        if (place.getParameters().containsKey(SHOW_EARLIEST_ACTIVE)) {
            place.removeParameter(SHOW_EARLIEST_ACTIVE);  // The token is a one-shot, meaning that it has to be deleted from the place
            commonInjector.getJobStoreProxyAsync().fetchEarliestActiveJob(
                    Integer.valueOf(place.getParameter(SinkJobFilter.class.getSimpleName())),
                    new FetchEarliestActiveJobAsyncCallback()
            );
        }
    }

    private void setPanelBackgrounds(String mainPanelBackground, String navigationPanelBackGround, String applicationPanelBackground) {
        final MainPanel mainPanel = (MainPanel) Document.get().getElementById(MainPanel.GUIID_MAIN_PANEL).getPropertyObject(MainPanel.GUIID_MAIN_PANEL);
        mainPanel.setBackgroundImage(mainPanelBackground);
        mainPanel.setNavigationPanelBackgroundColor(navigationPanelBackGround);
        mainPanel.setApplicationPanelBackgroundColor(applicationPanelBackground);
    }

    private void setupChangeColorSchemeListBox() {
        view.changeColorSchemeListBox.clear();
        for (Background background : Background.values()) {
            view.changeColorSchemeListBox.addItem(background.name().toLowerCase(), background.name());
        }
    }


    /*
     * Private methods
     */

    private void handleMultipleRerunErrorScenarios(JobModel jobModel, JobRerunScheme jobRerunScheme) {
        if (jobModel.isDiagnosticFatal()) {
            setLogMessage(LogPanelMessages.rerunCanceledFatalDiagnostic(jobModel.getJobId()));
        } else if (jobModel.getStateModel().getFailedCounter() == 0) {
            setLogMessage(LogPanelMessages.rerunCanceledNoFailed(jobModel.getJobId()));
        } else if (view.popupSelectBox.isRightSelected() && jobRerunScheme.getType() == JobRerunScheme.Type.TICKLE) {
            setLogMessage(LogPanelMessages.rerunCanceledTickle(jobModel.getJobId()));
        }
    }

    /**
     * validates if the job id is in a valid format (not empty and numeric)
     *
     * @return true if the format is valid, otherwise false
     */
    private boolean isJobIdValid() {
        if (jobId.isEmpty()) {
            view.setErrorText(texts.error_InputFieldValidationError());
            return false;
        }
        return isJobIdValidNumber();
    }

    /**
     * Validates that the job id is numeric
     *
     * @return true if the job id is numeric, otherwise false
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isJobIdValidNumber() {
        try {
            Long.valueOf(jobId);
        } catch (NumberFormatException e) {
            view.setErrorText(texts.error_NumericInputFieldValidationError());
            return false;
        }
        return true;
    }

    /**
     * Fetches count for a specific job id
     */
    private void countExistingJobsWithJobId() {
        JobListCriteria jobListCriteria = new JobListCriteria().where(new ListFilter<>(
                JobListCriteria.Field.JOB_ID,
                ListFilter.Op.EQUAL,
                Long.valueOf(jobId).intValue()));

        commonInjector.getJobStoreProxyAsync().countJobs(jobListCriteria, new CountExistingJobsWithJobIdCallBack());
    }

    /**
     * Creates a new workflow note model.
     * If input is not null, the method maps the values from input to the new workflow note model
     *
     * @param current the current workflow note model or null
     * @return workflowNoteModel
     */
    private WorkflowNoteModel mapValuesToWorkflowNoteModel(WorkflowNoteModel current) {
        final WorkflowNoteModel workflowNoteModel = new WorkflowNoteModel();
        if (current != null) {
            workflowNoteModel.setProcessed(current.isProcessed());
            workflowNoteModel.setDescription(current.getDescription());
        }
        return workflowNoteModel;
    }


    /**
     * Abstract Methods
     */

    protected abstract void updateBaseQuery();


    /*
     * Callback classes
     */

    class CountExistingJobsWithJobIdCallBack extends FilteredAsyncCallback<Long> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(Long count) {
            if (count == 0) {
                view.setErrorText(texts.error_JobNotFound());
            } else {
                view.jobIdInputField.setText("");
                placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(jobId, null));
            }
        }
    }

    class SetWorkflowNoteCallBack extends FilteredAsyncCallback<JobModel> {
        @Override
        public void onFilteredFailure(Throwable throwable) {
            view.setErrorText(throwable.getClass().getName() + " - " + throwable.getMessage());
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            view.selectionModel.setSelected(jobModel, true);
        }
    }

    private class FetchEarliestActiveJobAsyncCallback implements AsyncCallback<JobModel> {
        @Override
        public void onFailure(Throwable throwable) {
            // If no Earliest Active Jobs were found, do nothing - there is no job to select
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            if (jobModel != null) {
                view.selectionModel.setSelected(jobModel, true);
            }
        }
    }

    private LogPanel getLogPanel() {
        return ((ContentPanel) Document.get().getElementById(GUIID_CONTENT_PANEL).getPropertyObject(GUIID_CONTENT_PANEL)).getLogPanel();
    }

    private void setLogMessage(String message) {
        getLogPanel().showMessage(message);
    }
}
