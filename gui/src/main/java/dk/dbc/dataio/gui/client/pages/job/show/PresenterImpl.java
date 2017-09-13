/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.components.jobfilter.SinkJobFilter;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.texts.LogMessageTexts;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.places.AbstractBasePlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.client.views.MainPanel;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;
import dk.dbc.dataio.jobstore.types.criteria.ListFilterGroup;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dk.dbc.dataio.gui.client.views.ContentPanel.GUID_LOG_PANEL;


/**
 * This class represents the show jobs presenter implementation
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    public final static String SHOW_EARLIEST_ACTIVE = "ShowEarliestActive";

    private static final String TRANSPARENT = "transparent";
    private static final String EMPTY = "";
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private PlaceController placeController;
    private String jobId;
    private String header;
    protected View view;
    private Texts texts;
    LogMessageTexts logMessageTexts;
    private SinkContent.SinkType sinkType;
    boolean isRerunAllSelected = false;
    ContentPanel.LogPanel logPanel;

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
        this.logMessageTexts = view.getLogMessageTexts();
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
        if (!isRerunAllSelected) {
            editJob(view.popupSelectBox.isRightSelected(), sinkType);
        } else {
            rerunJobs(getShownJobModels(), view.popupSelectBox.isRightSelected());
        }
        // Return to default value (rerunAll all items selected)
        view.popupSelectBox.setRightSelected(false);
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
    public void setRerunAllSelected(boolean rerunAllSelected) {
        this.isRerunAllSelected = rerunAllSelected;
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

    /**
     * Method used to goto the job specification editor in order to rerunAll the selected job
     *
     * @param failedItemsOnlySelected determining whether all items or only failed should be rerunAll
     * @param sinkType                the type of sink
     */
    @Override
    public void editJob(boolean failedItemsOnlySelected, SinkContent.SinkType sinkType) {
        final JobModel jobModel = view.selectionModel.getSelectedObject();
        this.jobId = jobModel.getJobId();
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.job.modify.EditPlace(jobModel, failedItemsOnlySelected, sinkType));
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
    public void editJob(JobModel jobModel) {
        if(jobModel.getSinkId() == 0) {
            placeController.goTo(new dk.dbc.dataio.gui.client.pages.job.modify.EditPlace(jobModel, false, null));
        } else {
            commonInjector.getFlowStoreProxyAsync().getSink(jobModel.getSinkId(), new GetSinkFilteredAsyncCallback(jobModel, jobModel.hasFailedOnlyOption()));
        }
    }

    /**
     * Re runs a series of jobs, based on the input list job models
     *
     * @param jobModels The JobModels to rerunAll
     */
    @Override
    public void rerunJobs(List<JobModel> jobModels, boolean failedItemsOnlySelected) {
        // Due to History.back() from the edit job page, we have to make sure we have the correct logPanel...
        logPanel = (ContentPanel.LogPanel) Document.get().getElementById(GUID_LOG_PANEL).getPropertyObject(GUID_LOG_PANEL);
        logPanel.clearLogMessage();

        for (JobModel jobModel : jobModels) {
            if (failedItemsOnlySelected && !jobModel.hasFailedOnlyOption()) {
                logPanel.getLogMessageBuilder().append(logMessageTexts.log_rerunCanceledNoFailed().replace("$1", jobModel.getJobId()));
                logPanel.setLogMessage();
            } else {
                commonInjector.getFlowStoreProxyAsync().getSink(jobModel.getSinkId(), new GetSinkFilteredAsyncCallback(jobModel, failedItemsOnlySelected));
            }
        }
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
            logPanel.getLogMessageBuilder().append(caught.getMessage());
            logPanel.setLogMessage();
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            logPanel.getLogMessageBuilder().append(logMessageTexts.log_rerunFileStore().replace("$1", jobModel.getJobId()).replace("$2", oldJobId));
            logPanel.setLogMessage();
        }
    }

    /**
     * Call back class to be instantiated in the call to createJobRerun in jobstore proxy (RR)
     */
    class CreateJobRerunAsyncCallback implements AsyncCallback<Void> {
        private final String oldJobId;
        private String msg = logMessageTexts.log_allItems();

        CreateJobRerunAsyncCallback(String oldJobId, boolean failedItemsOnly) {
            this.oldJobId = oldJobId;
            if (failedItemsOnly) {
                msg = logMessageTexts.log_failedItems();
            }
        }

        @Override
        public void onFailure(Throwable caught) {
            logPanel.getLogMessageBuilder().append(caught.getMessage());
            logPanel.setLogMessage();
        }

        @Override
        public void onSuccess(Void result) {
            logPanel.getLogMessageBuilder().append(logMessageTexts.log_rerunJobStore().replace("$1", msg).replace("$2", oldJobId));
            logPanel.setLogMessage();
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
                placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(jobId));
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

    class GetSinkFilteredAsyncCallback extends FilteredAsyncCallback<SinkModel> {
        private final JobModel jobModel;
        private final boolean failedItemsOnly;

        GetSinkFilteredAsyncCallback(JobModel jobModel, boolean failedItemsOnly) {
            this.jobModel = jobModel;
            this.failedItemsOnly = failedItemsOnly;
        }

        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_SinkNotFoundError());
        }

        @Override
        public void onSuccess(SinkModel sinkModel) {
            sinkType = sinkModel.getSinkType();
            if (isRerunAllSelected) {
                rerunAll();
            } else {
                rerunSingle(sinkType);
            }
        }

        private void rerunAll() {
            if (jobModel.isResubmitJob() || sinkType == SinkContent.SinkType.TICKLE) {
                commonInjector.getJobStoreProxyAsync().reSubmitJob(jobModel, new ReSubmitJobFilteredAsyncCallback(jobModel.getJobId()));
            } else {
                commonInjector.getJobStoreProxyAsync().createJobRerun(Long.valueOf(jobModel.getJobId()).intValue(), failedItemsOnly, new CreateJobRerunAsyncCallback(jobModel.getJobId(), failedItemsOnly));
            }
        }

        private void rerunSingle(SinkContent.SinkType sinkType) {
            if (sinkType == SinkContent.SinkType.TICKLE || jobModel.getStateModel().getFailedCounter() == 0) {
                editJob(false, sinkType);
            } else {
                view.popupSelectBox.show();
            }
        }
    }
}
