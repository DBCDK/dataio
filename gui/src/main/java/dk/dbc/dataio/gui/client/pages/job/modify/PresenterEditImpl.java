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

package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.List;

/**
 * Concrete Presenter Implementation Class for Job Edit
 */
public class PresenterEditImpl <Place extends EditPlace> extends PresenterImpl {
    private Long jobId;

    /**
     * Constructor
     * @param place     the place
     * @param header    Breadcrumb header text
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        jobId = place.getJobId();
    }

    /**
     * Initializing the model
     */

    @Override
    protected void initializeViewFields() {
        View view = getView();
        boolean notRawRepo = !isRawRepo();
        view.jobId.setEnabled(false);
        view.packaging.setEnabled(notRawRepo);
        view.format.setEnabled(notRawRepo);
        view.charset.setEnabled(notRawRepo);
        view.destination.setEnabled(notRawRepo);
        view.mailForNotificationAboutVerification.setEnabled(notRawRepo);
        view.mailForNotificationAboutProcessing.setEnabled(notRawRepo);
        view.resultMailInitials.setEnabled(notRawRepo);
        view.type.setEnabled(notRawRepo);
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
     * ReSubmit Job
     * Updates the embedded model and resubmits the job
     */
    @Override
    void doReSubmitJobInJobStore() {
        if (isRawRepo()) {
            commonInjector.getJobStoreProxyAsync().createJobRerun(jobId.intValue(), new CreateJobRerunAsyncCallback());
        } else {
            commonInjector.getJobStoreProxyAsync().reSubmitJob(this.jobModel, new ReSubmitJobFilteredAsyncCallback() );
        }
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
            if (jobModels != null  && jobModels.size() > 0) {
                setJobModel(jobModels.get(0));
                updateAllFieldsAccordingToCurrentState();
            }
        }
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
            callbackOnSuccess(jobModel.getJobId());
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
            callbackOnSuccess("");  // No job id is given at this stage
        }
    }

    private void callbackOnFailure(Throwable caught) {
        String msg = "jobId: " + jobId;
        getView().setErrorText(ProxyErrorTranslator.toClientErrorFromJobStoreProxy(caught, commonInjector.getProxyErrorTexts(), msg));
    }

    private void callbackOnSuccess(String jobId) {
        final Texts texts = getTexts();
        getView().status.setText(texts.status_JobSuccesfullyRerun());
        Window.alert(texts.text_Job() + " " + (jobId.isEmpty() ? "" : jobId + " ") + texts.text_Created());
        History.back();
    }

}