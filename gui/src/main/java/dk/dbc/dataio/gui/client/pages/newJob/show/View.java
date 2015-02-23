package dk.dbc.dataio.gui.client.pages.newJob.show;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
* This class is the View class for the New Jobs Show View
*/
public class View extends ViewWidget {

    private static Resources resources;

    ColumnSortEvent.ListHandler<JobModel> columnSortHandler;
    Column jobCreationTimeColumn;
    ListDataProvider<JobModel> dataProvider;

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
    public View(String header, Texts texts, Resources resources) {
        super(header, texts);
        View.resources = resources;
        setupColumns();
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
        jobsTable.setPageSize(PAGE_SIZE);
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
        jobsTable.addColumn(constructSuccessCounterColumn(), texts.columnHeader_SuccessCounter());
        jobsTable.addColumn(constructFailedCounterColumn(), texts.columnHeader_FailureCounter());
        jobsTable.addColumn(constructIgnoredCounterColumn(), texts.columnHeader_IgnoredCounter());
        jobsTable.addColumn(constructJobStateColumn(), texts.columnHeader_JobStatus());
        pagerTop.setDisplay(jobsTable);
        pagerBottom.setDisplay(jobsTable);
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
     * This method constructs the success counter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed success count column
     */
    Column constructSuccessCounterColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return String.valueOf(model.getSucceededCounter());
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareLongs(
                        o1.getSucceededCounter(), o2.getSucceededCounter()) : 1;

            }
        });
        return column;
    }

    /**
     * This method constructs the failed counter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed failed count column
     */
    Column constructFailedCounterColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return String.valueOf(model.getFailedCounter());
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareLongs(
                        o1.getFailedCounter(), o2.getFailedCounter()) : 1;
            }
        });
        return column;
    }

    /**
     * This method constructs the ignored counter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed ignored count column
     */
    Column constructIgnoredCounterColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return String.valueOf(model.getIgnoredCounter());
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return ViewHelper.validateObjects(o1, o2) ? ViewHelper.compareLongs(
                                o1.getIgnoredCounter(), o2.getIgnoredCounter()) : 1;
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
        return new StatusColumn(resources, statusCell);
    }

}
