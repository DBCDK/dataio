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
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.List;


/**
* This class represents the show jobs presenter implementation
*/
public abstract class PresenterImpl extends AbstractActivity implements Presenter {

    private static final String EMPTY = "";
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    View globalJobsView;
    private PlaceController placeController;
    private String jobId;
    private String header;

    /**
     * Default constructor
     *
     * @param placeController   PlaceController for navigation
     * @param globalJobsView    Global Jobs View, necessary for keeping filter state etc.
     * @param header            Breadcrumb header text
     */
    public PresenterImpl(PlaceController placeController, View globalJobsView, String header) {
        this.placeController = placeController;
        this.globalJobsView = globalJobsView;
        this.header = header;
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
        getView().setPresenter(this);
        getView().setHeader(this.header);
        containerWidget.setWidget(getView().asWidget());
        updateBaseQuery();
        refresh();
    }

    /**
     * This method is a result of a click on one job in the list, and activates the Item Show page
     * @param model The model, containing the selected item
     */
    @Override
    public void itemSelected(JobModel model) {
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(model.getJobId()));
    }

    @Override
    public void updateSelectedJobs() {
        View view = getView();
        view.selectionModel.clear();
        view.dataProvider.updateCurrentCriteria();
        view.refreshJobsTable();
    }


    @Override
    public void refresh() {
        getView().refreshJobsTable();
    }

    /**
     * Method used to look up a specified job and display the job in the items show view
     */
    @Override
    public void showJob() {
        this.jobId = getView().jobIdInputField.getValue().trim();
        if(isJobIdValid()) {
            countExistingJobsWithJobId();
        }
    }
    public void editJob(JobModel jobRow) {
        this.jobId = jobRow.getJobId();
        placeController.goTo(new dk.dbc.dataio.gui.client.pages.job.modify.EditPlace(jobId));
    }

    @Override
    public void setWorkflowNote(WorkflowNoteModel workflowNoteModel, String jobId) {
        commonInjector.getJobStoreProxyAsync().setWorkflowNote(workflowNoteModel, Long.valueOf(jobId).intValue(), new SetWorkflowNoteCallBack());
    }

    /**
     * This method evaluates the assignee given as input.
     * If the assignee is empty, an error is displayed in the view.
     * Otherwise the assignee input value is set on a new workflow note model.
     *
     * @param assignee the assignee to set
     * @return null if the assignee value was empty, otherwise a new workflow note model with the input string set as assignee.
     */
    @Override
    public WorkflowNoteModel preProcessAssignee(String assignee) {
        WorkflowNoteModel workflowNoteModel = null;
        if (assignee.trim().equals(EMPTY)) {
            getView().setErrorText(getView().getTexts().error_CheckboxCellValidationError());
        } else {
            workflowNoteModel = mapValuesToWorkflowNoteModel(getView().selectionModel.getSelectedObject().getWorkflowNoteModel());
            workflowNoteModel.setAssignee(assignee.trim().toUpperCase());
        }
        return workflowNoteModel;
    }

    /**
     * Re runs a series of jobs, based on the input list job models
     * @param jobModels The JobModels to rerun
     */
    public void rerunJobs(List<JobModel> jobModels) {
        for (JobModel model: jobModels) {
            commonInjector.getJobStoreProxyAsync().reRunJob(model, new RerunJobFilteredAsyncCallback() );
        }
    }


    /*
     * Private methods
     */

    View getView() {
        return this.globalJobsView;
    }

    /**
     * validates if the job id is in a valid format (not empty and numeric)
     * @return true if the format is valid, otherwise false
     */
    private boolean isJobIdValid() {
        if(jobId.isEmpty()) {
            getView().setErrorText(getView().getTexts().error_InputFieldValidationError());
            return false;
        }
        return isJobIdValidNumber();
    }

    /**
     * Validates that the job id is numeric
     * @return true if the job id is numeric, otherwise false
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean isJobIdValidNumber() {
             try {
            Long.valueOf(jobId);
        } catch (NumberFormatException e) {
            getView().setErrorText(getView().getTexts().error_NumericInputFieldValidationError());
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
        if(current != null) {
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

    protected class CountExistingJobsWithJobIdCallBack extends FilteredAsyncCallback<Long> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(Long count) {
            if (count == 0) {
                getView().setErrorText(getView().getTexts().error_JobNotFound());
            } else {
                getView().jobIdInputField.setText("");
                placeController.goTo(new dk.dbc.dataio.gui.client.pages.item.show.Place(jobId));
            }
        }
    }

    protected class SetWorkflowNoteCallBack extends FilteredAsyncCallback<JobModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(e.getClass().getName() + " - " + e.getMessage());
        }

        @Override
        public void onSuccess(JobModel jobModel) {
            getView().selectionModel.setSelected(jobModel, true);
        }
    }

    protected class RerunJobFilteredAsyncCallback extends FilteredAsyncCallback<JobModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "jobId: " + jobId;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromJobStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(JobModel jobModel) {
        }
    }

}