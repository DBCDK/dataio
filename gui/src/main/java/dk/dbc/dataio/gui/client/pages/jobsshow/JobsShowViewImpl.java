package dk.dbc.dataio.gui.client.pages.jobsshow;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.resource.Resources;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Show Jobs view implementation
 * Shows a table, containing:
 *  o Job creation Time
 *  o Job ID
 *  o Filename
 *  o Submitternumber
 */
public class JobsShowViewImpl extends ContentPanel<JobsShowPresenter> implements JobsShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_JOBS_SHOW_WIDGET = "jobsshowwidget";
    public static final String GUIID_JOBS_MORE_BUTTON = "jobsshowmorebutton";
    public static final String GUICLASS_GRAY = "gray-lamp";
    public static final String GUICLASS_GREEN = "green-lamp";
    public static final String GUICLASS_RED = "red-lamp";

    // Enums
    private enum JobStatusEnum {NOT_DONE, DONE_WITH_ERROR, DONE_WITHOUT_ERROR}

    // Configuration constants
    private static final int PAGE_SIZE = 20;

    private Resources resources = GWT.create(Resources.class);

    // Local variables
    private final static JobsShowConstants constants = GWT.create(JobsShowConstants.class);
    private final DioCellTable<JobInfo> table = new DioCellTable<JobInfo>();
    private final Button showMoreButton = new Button(constants.button_ShowMore());
    ListDataProvider<JobInfo> dataProvider = new ListDataProvider<JobInfo>();
    TextColumn<JobInfo> jobIdColumn;
    TextColumn<JobInfo> fileNameColumn;
    TextColumn<JobInfo> submitterNumberColumn;
    TextColumn<JobInfo> jobCreationTimeColumn;
    Column<JobInfo, ImageResource> jobStateColumn;

    private int currentPageSize = PAGE_SIZE;

    /**
     * Constructor
     */
    public JobsShowViewImpl() {
        super(constants.menu_Jobs());
    }

    /**
     * Initializations of the view
     * Sets up the three columns in the CellTable
     */
    public void init() {
        table.updateStarted();

        getElement().setId(GUIID_JOBS_SHOW_WIDGET);

        // Job Creation Time Column
        jobCreationTimeColumn = new TextColumn<JobInfo>() {
            @Override
            public String getValue(JobInfo content) {
                return Format.getLongDateTimeFormat(content.getJobCreationTime());
            }
        };
        jobCreationTimeColumn.setSortable(true);
        table.addColumn(jobCreationTimeColumn, constants.columnHeader_JobCreationTime());

        // Job ID Column
        jobIdColumn = new TextColumn<JobInfo>() {
            @Override
            public String getValue(JobInfo content) {
                return getJobIdColumn(content);
            }
        };
        jobIdColumn.setSortable(true);
        table.addColumn(jobIdColumn, constants.columnHeader_JobId());

        // File Name Column
        fileNameColumn = new TextColumn<JobInfo>() {
            @Override
            public String getValue(JobInfo content) {
                return getFileNameColumn(content);
            }
        };
        fileNameColumn.setSortable(true);
        table.addColumn(fileNameColumn, constants.columnHeader_FileName());

        // Submitter Number Column
        submitterNumberColumn = new TextColumn<JobInfo>() {
            @Override
            public String getValue(JobInfo content) {
                return getSubmitterNumberColumn(content);
            }
        };
        submitterNumberColumn.setSortable(true);
        table.addColumn(submitterNumberColumn, constants.columnHeader_SubmitterNumber());

        // Job State Column
        jobStateColumn = new Column<JobInfo, ImageResource>(new ImageResourceCell()) {
            @Override
            public String getCellStyleNames(Cell.Context context, JobInfo content) {
                switch (getJobStatus(content)) {
                    case DONE_WITHOUT_ERROR:
                        return GUICLASS_GREEN;
                    case DONE_WITH_ERROR:
                        return GUICLASS_RED;
                    default:
                    case NOT_DONE:
                        return GUICLASS_GRAY;
                }
            }
            @Override
            public ImageResource getValue(JobInfo content) {
                switch (getJobStatus(content)) {
                    case DONE_WITHOUT_ERROR:
                        return resources.green();
                    case DONE_WITH_ERROR:
                        return resources.red();
                    default:
                    case NOT_DONE:
                        return resources.gray();
                }
            }
        };
        table.addColumn(jobStateColumn, constants.columnHeader_JobStatus());

        // Add table to view
        add(table);

        // Connect the table to the data provider.
        dataProvider.addDataDisplay(table);

        showMoreButton.getElement().setId(GUIID_JOBS_MORE_BUTTON);
        showMoreButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                increasePageSize();
            }
        });
        add(showMoreButton);
    }

    /**
     * This method maps the logical jobstatus to a JobStatusEnum
     * @param jobInfo The JobInfo for the job to test
     * @return JobStatusEnum: NOT_DONE, DONE_WITHOUT_ERROR or DONE_WITH_ERROR
     */
    private JobStatusEnum getJobStatus(JobInfo jobInfo) {
        if (jobInfo.getJobErrorCode() != JobErrorCode.NO_ERROR ) {
            return JobStatusEnum.DONE_WITH_ERROR;
        }
        // Now we know, that JobErrorCode is JobErrorCode.NO_ERROR
        if (jobInfo.getChunkifyingChunkCounter() == null ||
                jobInfo.getProcessingChunkCounter() == null ||
                jobInfo.getDeliveringChunkCounter() == null ) {
            return JobStatusEnum.NOT_DONE;
        }
        // Now we know, that the three counter have values, and we can now check the counters
        if (jobInfo.getChunkifyingChunkCounter().getItemResultCounter().getFailure() > 0L) {
            return JobStatusEnum.DONE_WITH_ERROR;
        }
        if (jobInfo.getProcessingChunkCounter().getItemResultCounter().getFailure() > 0L) {
            return JobStatusEnum.DONE_WITH_ERROR;
        }
        if (jobInfo.getDeliveringChunkCounter().getItemResultCounter().getFailure() > 0L) {
            return JobStatusEnum.DONE_WITH_ERROR;
        }
        // We couldn't find any errors, so return with DONE_WITHOUT_ERROR
        return JobStatusEnum.DONE_WITHOUT_ERROR;
    }

    /*
     * Implementation of interface methods
     */

    /**
     * Refresh
     */
    @Override
    public void refresh() {
    }

    /**
     * Clear all fields in this view
     */
    @Override
    public void clearFields() {
    }

    /**
     * OnSuccess
     * @param message The message to display to the user
     */
    @Override
    public void onSuccess(String message) {
    }

    /**
     * OnFailure
     * @param message The message to display to the user
     */
    @Override
    public void onFailure(String message) {
        super.onFailure(message);
        table.updateDone();
    }

    /**
     * setJobs is called by the presenter, to push table data to the view
     * @param jobs List of jobs to view
     */
    @Override
    public void setJobs(List<JobInfo> jobs) {

        // Link the Data Provider object to this list of jobs
        dataProvider.setList(jobs);

        // Add a ColumnSortEvent.ListHandler to connect sorting to the java.util.List
        ColumnSortEvent.ListHandler<JobInfo> columnSortHandler = new ColumnSortEvent.ListHandler<JobInfo>(dataProvider.getList()) {
            @Override
            public void onColumnSort(ColumnSortEvent event) {
                Collections.sort(dataProvider.getList(), Collections.reverseOrder(getComparator(jobCreationTimeColumn)));  // Do sort jobCreationTimeColumn first, to assure, that the secondary search will be by jobCreationTimeColumn
                super.onColumnSort(event);
            }
        };

        columnSortHandler.setComparator(jobCreationTimeColumn,
                new Comparator<JobInfo>() {
                    public int compare(JobInfo o1, JobInfo o2) {
                        return validateObjects(o1, o2) ? compareLongs(o1.getJobCreationTime(), o2.getJobCreationTime()) : 1;
                    }
                });

        columnSortHandler.setComparator(jobIdColumn,
                new Comparator<JobInfo>() {
                    public int compare(JobInfo o1, JobInfo o2) {
                        return validateObjects(o1, o2) ? compareLongs(o1.getJobId(), o2.getJobId()) : 1;
                    }
                });
        columnSortHandler.setComparator(fileNameColumn,
                new Comparator<JobInfo>() {
                    public int compare(JobInfo o1, JobInfo o2) {
                        return validateObjects(o1, o2) ? compareStrings(o1.getJobSpecification().getDataFile(), o2.getJobSpecification().getDataFile()) : 1;
                    }
                });
        columnSortHandler.setComparator(submitterNumberColumn,
                new Comparator<JobInfo>() {
                    public int compare(JobInfo o1, JobInfo o2) {
                        return validateObjects(o1, o2) ? compareLongs(o1.getJobSpecification().getSubmitterId(), o2.getJobSpecification().getSubmitterId()) : 1;
                    }
                });
        table.addColumnSortHandler(columnSortHandler);

        // Set default sort behavior
        jobCreationTimeColumn.setDefaultSortAscending(false);  // Set default sort order for jobCreationTime Column to Descending (youngest first)

        ColumnSortList columnSortList = table.getColumnSortList();
        columnSortList.clear();  // Clear the Sort List
        columnSortList.push(jobCreationTimeColumn);  // Default sorting is by job creation time
        ColumnSortEvent.fire(table, columnSortList);  // Do sort right now

        // Set page size parameters
        currentPageSize = PAGE_SIZE;
        table.setPageSize(currentPageSize);
        table.setRowCount(jobs.size());
        table.updateDone();
    }

    /**
     * Increases currentPageSize to show one more page
     */
    public void increasePageSize() {
        int newPageSize = currentPageSize + PAGE_SIZE;
        if (newPageSize > table.getRowCount()) {
            currentPageSize = table.getRowCount();
        } else {
            currentPageSize = newPageSize;
        }
        table.setPageSize(currentPageSize);
    }


    // Private methods

    private String getJobIdColumn(JobInfo content) {
        return Long.toString(content.getJobId());
    }

    private String getFileNameColumn(JobInfo content) {
        return content.getJobSpecification().getDataFile().replaceFirst("^/tmp/", "");
    }

    private String getSubmitterNumberColumn(JobInfo content) {
        return Long.toString(content.getJobSpecification().getSubmitterId());
    }

    private boolean validateObjects(Object o1, Object o2) {
        return o1 != null && o2 != null;
    }

    private int compareStrings(String s1, String s2) {
        return s1.compareTo(s2);
    }

    private int compareLongs(long l1, long l2) {
        return (l1 == l2) ? 0 : (l1 < l2) ? -1 : 1;
    }
}
