package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.panels.statuspopup.StatusPopupEvent;
import dk.dbc.dataio.gui.client.resource.ImageResources;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class is the View class for the Jobs Show View
 */
public class View extends ViewWidget {

    // Instantiate Event Binder for the Status Popup Panel
    interface MyEventBinder extends EventBinder<View> {
    }

    private final MyEventBinder statusPopupEventBinder = GWT.create(MyEventBinder.class);
    private final static EventBus statusPopupEventBus = new SimpleEventBus();

    private static ImageResources imageResources;

    int currentPageSize = PAGE_SIZE;
    ColumnSortEvent.ListHandler<JobModel> columnSortHandler;
    Column jobCreationTimeColumn;
    ListDataProvider<JobModel> dataProvider;

    // Constants
    private static final int PAGE_SIZE = 20;

    // Enums
    enum JobStatus {
        NOT_DONE, DONE_WITH_ERROR, DONE_WITHOUT_ERROR
    }


    /**
     * Default constructor
     *
     * @param header The header text for the View
     * @param texts  The I8n texts for this view
     */
    public View(String header, Texts texts, ImageResources imageResources) {
        super(header, texts);
        View.imageResources = imageResources;
        statusPopupEventBinder.bindEventHandlers(this, statusPopupEventBus);
        setupColumns();
    }



    /*
     * Event Handlers
     */

    /**
     * Concrete implementation of abstract Event Handler for the More Button
     * Should have been private, but is package-private to enable unit test
     *
     * @param event The event, triggered by a push on the More Button
     */
    @Override
    void moreInfoButtonPressedEvent(ClickEvent event) {
        increasePageSize();
    }

    /**
     * Event Handler for events from the Status Popup Panel
     */
    @EventHandler
    void statusPopupEvent(StatusPopupEvent event) {
        switch (event.getStatusPopupEventType()) {
            case TOTAL_STATUS_INFO:
                presenter.showFailedItems(String.valueOf(event.getJobId()), null, null);
                break;
            case MORE_INFORMATION_REQUESTED:
                presenter.showMoreInformation(event.getJobId());
                break;
            case DETAILED_STATUS:
                presenter.showFailedItems(String.valueOf(event.getJobId()), event.getOperationalState(), event.getCompletionState());
                break;
        }
    }


    /*
     * Public methods
     */

    /**
     * This method is used to put data into the view
     *
     * @param jobs The list of jobs to put into the view
     */
    public void setJobs(List<JobModel> jobs) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(jobs);

        // Do sort by job creation time
        ColumnSortList columnSortList = jobsTable.getColumnSortList();
        columnSortList.clear();  // Clear the Sort List
        columnSortList.push(jobCreationTimeColumn);  // Default sorting is by job creation time
        ColumnSortEvent.fire(jobsTable, columnSortList);  // Do sort right now

        // Set page size parameters
        currentPageSize = PAGE_SIZE;
        jobsTable.setPageSize(currentPageSize);
        jobsTable.setRowCount(jobs.size());
    }


    /**
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setJobs method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<JobModel>();
        dataProvider.addDataDisplay(jobsTable);

        columnSortHandler = new ColumnSortEvent.ListHandler<JobModel>(dataProvider.getList()) {
            @Override
            public void onColumnSort(ColumnSortEvent event) {
                // Prior to each sort, do sort jobCreationTimeColumn first, to assure, that the secondary search will be by jobCreationTimeColumn
                Collections.sort(dataProvider.getList(), Collections.reverseOrder(getComparator(jobCreationTimeColumn)));
                super.onColumnSort(event);
            }
        };
        jobsTable.addColumnSortHandler(columnSortHandler);

        jobsTable.addColumn(jobCreationTimeColumn = constructJobCreationTimeColumn(), texts.columnHeader_JobCreationTime());
        jobsTable.addColumn(constructJobIdColumn(), texts.columnHeader_JobId());
        jobsTable.addColumn(constructFileNameColumn(), texts.columnHeader_FileName());
        jobsTable.addColumn(constructSubmitterNumberColumn(), texts.columnHeader_SubmitterNumber());
        jobsTable.addColumn(constructChunkCountColumn(), texts.columnHeader_TotalChunkCount());
        jobsTable.addColumn(constructJobStateColumn(), texts.columnHeader_JobStatus());
    }

    /**
     * This method constructs the JobCreationTime column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobCreationTime column
     */
    Column constructJobCreationTimeColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getJobCreationTime();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareLongDates(o1.getJobCreationTime(), o2.getJobCreationTime()) : 1;
            }
        });
        column.setDefaultSortAscending(false);  // Set default sort order for jobCreationTime Column to Descending (youngest first)
        return column;
    }

    /**
     * This method constructs the JobId column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobId column
     */
    Column constructJobIdColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getJobId();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareStringsAsLongs(o1.getJobId(), o2.getJobId()) : 1;
            }
        });
        return column;
    }

    /**
     * This method constructs the FileName column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed FileName column
     */
    Column constructFileNameColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getFileName();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareStrings(o1.getFileName(), o2.getFileName()) : 1;
            }
        });
        return column;
    }

    /**
     * This method constructs the SubmitterNumber column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SubmitterNumber column
     */
    Column constructSubmitterNumberColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getSubmitterNumber();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareStringsAsLongs(o1.getSubmitterNumber(), o2.getSubmitterNumber()) : 1;
            }
        });
        return column;
    }

    /**
     * This method constructs the ChunkCount column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed ChunkCount column
     */
    Column constructChunkCountColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return String.valueOf(model.getChunkifyingTotalCounter());
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareLongs(o1.getChunkifyingTotalCounter(), o2.getChunkifyingTotalCounter()) : 1;
            }
        });
        return column;
    }


    /**
     * This method constructs the JobState column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobState column
     */
    Column constructJobStateColumn() {
        final ImageResourceCell statusCell = new ImageResourceCell() {
            public Set<String> getConsumedEvents() {
                HashSet<String> events = new HashSet<String>();
                events.add("click");
                return events;
            }
        };
        return new StatusColumn(statusPopupEventBus, imageResources, statusCell);
    }


    /**
     * Increases currentPageSize to show one more page
     */
    private void increasePageSize() {
        int newPageSize = currentPageSize + PAGE_SIZE;
        if (newPageSize > jobsTable.getRowCount()) {
            currentPageSize = jobsTable.getRowCount();
        } else {
            currentPageSize = newPageSize;
        }
        jobsTable.setPageSize(currentPageSize);
    }


}
