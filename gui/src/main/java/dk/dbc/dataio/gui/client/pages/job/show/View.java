package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.CellPreviewEvent;
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

    Column jobCreationTimeColumn;
    public AsyncJobViewDataProvider dataProvider;
    SingleSelectionModel<JobModel> selectionModel = new SingleSelectionModel<>();

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
     * Force a refresh of the jobsTable data.
     */
    public void refreshJobsTable() {
        jobsTable.setVisibleRangeAndClearData(new Range(0, 20), true);
    }


    /**
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
        jobsTable.addCellPreviewHandler(new CellPreviewHandlerClass());

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

    class CellPreviewHandlerClass implements CellPreviewEvent.Handler<JobModel> {
        @Override
        public void onCellPreview(CellPreviewEvent<JobModel> cellPreviewEvent) {
            if(BrowserEvents.CLICK.equals(cellPreviewEvent.getNativeEvent().getType())) {
                selectionModel.setSelected(cellPreviewEvent.getValue(), true);
                presenter.itemSelected(selectionModel.getSelectedObject());
            }
        }
    }

}
