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

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.util.ClientFactory;


/**
* This class is the View class for the New Jobs Show View
*/
public class View extends ViewWidget {

    private static Resources resources;

    private boolean dataHasNotYetBeenLoaded = true;

    Column jobCreationTimeColumn;
    public AsyncJobViewDataProvider dataProvider;
    ProvidesKey<JobModel> keyProvider = new ProvidesKey<JobModel>() {
        @Override
        public Object getKey(JobModel jobModel) {
            return (jobModel == null) ? null : jobModel.getJobId();
        }
    };
    SingleSelectionModel<JobModel> selectionModel = new SingleSelectionModel<>(keyProvider);

    // Enums
    enum JobStatus {
        NOT_DONE, DONE_WITH_ERROR, DONE_WITHOUT_ERROR
    }


    /**
     * Default constructor
     *
     * @param clientFactory, the client factory
     * @param headerText, the text to display in the header
     */
    public View(ClientFactory clientFactory, String headerText) {
        this(clientFactory, headerText, true, true);
        dataProvider.addDataDisplay(jobsTable);
    }

    /* Package scoped Constructor used for unit testing. */
    View(ClientFactory clientFactory, String headerText, Boolean setupColumns, Boolean updateUserCriteria) {
        super(clientFactory, headerText);
        View.resources = clientFactory.getImageResources();
        if( setupColumns ) {
            setupColumns();
        }
        dataProvider = new AsyncJobViewDataProvider(clientFactory, this, updateUserCriteria);
    }


    /*
     * Public methods
     */

    /**
     * Refreshes the content of the Jobs Table
     */
    public void refreshJobsTable() {
        jobsTable.setVisibleRangeAndClearData(this.jobsTable.getVisibleRange(), true);
    }

    /**
     * Loads the content of the Jobs Table
     * If the table has been loaded, don't reload
     */
    public void loadJobsTable() {
        if (dataProvider != null && dataHasNotYetBeenLoaded) {
            dataHasNotYetBeenLoaded = false;
            jobsTable.setVisibleRangeAndClearData(new Range(0, 20), true);
        }
    }


    /*
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setJobs method
     */
    @SuppressWarnings("unchecked")
    void setupColumns() {
        jobsTable.addColumn(jobCreationTimeColumn = constructJobCreationTimeColumn(), texts.columnHeader_JobCreationTime());
        jobsTable.addColumn(constructJobIdColumn(), texts.columnHeader_JobId());
        jobsTable.addColumn(constructSubmitterNumberColumn(), texts.columnHeader_SubmitterNumber());
        jobsTable.addColumn(constructSubmitterNameColumn(), texts.columnHeader_SubmitterName());
        jobsTable.addColumn(constructFlowBinderNameColumn(), texts.columnHeader_FlowBinderName());
        jobsTable.addColumn(constructSinkNameColumn(), texts.columnHeader_SinkName());
        jobsTable.addColumn(constructItemCountColumn(), texts.columnHeader_TotalChunkCount());
        jobsTable.addColumn(constructFailedCounterColumn(), texts.columnHeader_FailureCounter());
        jobsTable.addColumn(constructIgnoredCounterColumn(), texts.columnHeader_IgnoredCounter());
        jobsTable.addColumn(constructProgressBarColumn(), texts.columnHeader_ProgressBar());
        jobsTable.addColumn(constructJobStateColumn(), texts.columnHeader_JobStatus());
        jobsTable.setSelectionModel(selectionModel);
        jobsTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());

        pagerTop.setDisplay(jobsTable);
        pagerBottom.setDisplay(jobsTable);

        jobsTable.setVisibleRange(0,20);


    }

    /**
     * This method constructs the JobCreationTime column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobCreationTime column
     */
    Column constructJobCreationTimeColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getJobCreationTime();
            }
        };
    }

    /**
     * This method constructs the JobId column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobId column
     */
    Column constructJobIdColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getJobId();
            }
        };
    }

    /**
     * This method constructs the SubmitterNumber column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SubmitterNumber column
     */
    Column constructSubmitterNumberColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getSubmitterNumber();
            }
        };
    }

    /**
     * This method constructs the SubmitterName column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SubmitterName column
     */
    Column constructSubmitterNameColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getSubmitterName();
            }
        };
    }

    /**
     * This method constructs the FlowBinderName column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed FlowBinderName column
     */
    Column constructFlowBinderNameColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getFlowBinderName();
            }
        };
    }

    /**
     * This method constructs the SinkName column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SinkName column
     */
    Column constructSinkNameColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getSinkName();
            }
        };
    }

    /**
     * This method constructs the item count column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed item count column
     */
    Column constructItemCountColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return String.valueOf(model.getItemCounter());
            }
        };
    }

    /**
     * This method constructs the failed counter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed failed count column
     */
    Column constructFailedCounterColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return String.valueOf(model.getFailedCounter());
            }
        };
    }

    /**
     * This method constructs the ignored counter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed ignored count column
     */
    Column constructIgnoredCounterColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return String.valueOf(model.getProcessingIgnoredCounter());
            }
        };
    }

    /**
     * This method constructs the progress bar column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed progress bar column
     */
    Column constructProgressBarColumn() {
        return new ProgressColumn();
    }

    /**
     * This method constructs the JobState column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobState column
     */
    Column constructJobStateColumn() {
        final ImageResourceCell statusCell = new ImageResourceCell();
        return new StatusColumn(resources, statusCell);
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler(){
        DoubleClickHandler handler = new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent doubleClickEvent) {
                JobModel selected = selectionModel.getSelectedObject();
                if(selected != null) {
                    presenter.itemSelected(selected);
                }
            }
        };
        return handler;
    }

}
