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
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.ListFilter;

import java.util.List;

/**
 * Concrete Presenter Implementation Class for Submitter Edit
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
     * The method fetches the stored Submitter, as given in the Place (referenced by this.id)
     */

    @Override
    protected void initializeViewFields() {
        View view = getView();
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
     * saveModel
     * Updates the embedded model as a Submitter in the database
     */
    @Override
    void doRerunJobInJobStore() {
        commonInjector.getJobStoreProxyAsync().addJob(this.jobModel, new RerunJobFilteredAsyncCallback() );
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
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(List<JobModel> jobModels) {

            System.out.println("Number of jobs found: " + jobModels.size());
            if (jobModels != null  && jobModels.size() > 0) {
                setJobModel(jobModels.get(0));
                updateAllFieldsAccordingToCurrentState();
            }
//            else {
                //prepareViewForJobNotFoundDisplay();
//            }
        }
    }

    /**
     * Call back class to be instantiated in the call to listJobs in jobstore proxy
     */
    class RerunJobFilteredAsyncCallback extends FilteredAsyncCallback<JobModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "jobId: " + jobId;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(JobModel jobModel) {

            System.out.println("Rerun of job: " + jobModel.getJobId());
            getView().status.setText(getTexts().status_JobSuccesfullyRerun());
            setJobModel(jobModel);
            updateAllFieldsAccordingToCurrentState();
            History.back();
        }
    }
}