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
    private JobListCriteria currentCriteria = new JobListCriteria();

    public AsyncJobViewDataProvider(ClientFactory clientFactory, View view_) {
        this(clientFactory, view_, true);
    }

    /* Package scoped Constructor used for unit testing. */
    AsyncJobViewDataProvider(ClientFactory clientFactory, View view_, Boolean updateUserCriteria) {
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        view = view_;
        if (updateUserCriteria) {
            updateUserCriteria();
        }
    }

    void setBaseCriteria( JobListCriteria newBaseCriteria) {
        baseCriteria = newBaseCriteria;
        updateCurrentCriteria();
    }


    void updateCurrentCriteria() {
        JobListCriteria newJobListCriteria = new JobListCriteria();


        if( baseCriteria != null) {
            newJobListCriteria.and(baseCriteria);
        }

        if (userCriteria != null) {
            newJobListCriteria.where(userCriteria);
        }

        if( !currentCriteria.equals(newJobListCriteria)) {
            criteriaIncarnation++;
            currentCriteria = newJobListCriteria;
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
            userCriteria = view.jobFilter.getValue();
            if (view.processingFailedJobsButton.getValue()) {
                userCriteria.where(new ListFilter<>(JobListCriteria.Field.STATE_PROCESSING_FAILED));

            } else if (view.deliveringFailedJobsButton.getValue()) {
                userCriteria.where(new ListFilter<>(JobListCriteria.Field.STATE_DELIVERING_FAILED));

            } else if (view.fatalJobsButton.getValue()) {
                userCriteria.where(new ListFilter<>(JobListCriteria.Field.WITH_FATAL_ERROR));
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

        currentCriteria.limit(range.getLength());
        currentCriteria.offset(range.getStart());


        jobStoreProxy.listJobs(currentCriteria, new FilteredAsyncCallback<List<JobModel>>() {
                    // protection against old calls updating the view with old data.
                    int criteriaIncarnationOnRequestCall=criteriaIncarnation;
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
                        return criteriaIncarnationOnRequestCall == criteriaIncarnation &&
                                offsetOnRequestCall == currentCriteria.getOffset();
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
