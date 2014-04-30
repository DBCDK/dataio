package dk.dbc.dataio.gui.client.pages.jobsshow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import java.util.List;

/**
 * Show Jobs view implementation
 * Shows a table, containing:
 *  o Job ID
 *  o Filename
 *  o Submitternumber
 */
public class JobsShowViewImpl extends ContentPanel<JobsShowPresenter> implements JobsShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_JOBS_SHOW_WIDGET = "jobsshowwidget";

    // Local variables
    private final static JobsShowConstants constants = GWT.create(JobsShowConstants.class);
    private final DioCellTable<JobInfo> table = new DioCellTable<JobInfo>();
    private final Button showMoreButton = new Button(constants.button_ShowMore());


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

        if (table.getColumnCount() == 0) {
            TextColumn<JobInfo> jobIdColumn = new TextColumn<JobInfo>() {
                @Override
                public String getValue(JobInfo content) {
                    return getJobIdColumn(content);
                }
            };
            table.addColumn(jobIdColumn, constants.columnHeader_JobId());

            TextColumn<JobInfo> fileNameColumn = new TextColumn<JobInfo>() {
                @Override
                public String getValue(JobInfo content) {
                    return getFileNameColumn(content);
                }
            };
            table.addColumn(fileNameColumn, constants.columnHeader_FileName());

            TextColumn<JobInfo> submitterNumberColumn = new TextColumn<JobInfo>() {
                @Override
                public String getValue(JobInfo content) {
                    return getSubmitterNumberColumn(content);
                }
            };
            table.addColumn(submitterNumberColumn, constants.columnHeader_SubmitterNumber());
            add(table);

            add(showMoreButton);
        }
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
        table.setPageSize(jobs.size());
        table.setRowData(0, jobs);
        table.setRowCount(jobs.size());
        table.updateDone();
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

}
