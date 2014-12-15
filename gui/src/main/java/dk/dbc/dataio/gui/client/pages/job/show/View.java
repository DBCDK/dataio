package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.event.shared.binder.EventBinder;
import com.google.web.bindery.event.shared.binder.EventHandler;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.panels.statuspopup.StatusPopup;
import dk.dbc.dataio.gui.client.panels.statuspopup.StatusPopupEvent;
import dk.dbc.dataio.gui.client.resource.Resources;
import dk.dbc.dataio.gui.client.util.Format;

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

    private static final Resources RESOURCES = GWT.create(Resources.class);

    private int currentPageSize = PAGE_SIZE;
    private ColumnSortEvent.ListHandler<JobModel> columnSortHandler;
    private Column jobCreationTimeColumn;
    private ListDataProvider<JobModel> dataProvider;

    // Constants
    private static final int PAGE_SIZE = 20;
    private static final String GUICLASS_GRAY = "gray-lamp";
    private static final String GUICLASS_GREEN = "green-lamp";
    private static final String GUICLASS_RED = "red-lamp";

    // Enums
    private enum JobStatus {
        NOT_DONE, DONE_WITH_ERROR, DONE_WITHOUT_ERROR
    }


    /**
     * Default constructor
     *
     * @param header The header text for the View
     * @param texts  The I8n texts for this view
     */
    public View(String header, Texts texts) {
        super(header, texts);
        statusPopupEventBinder.bindEventHandlers(this, statusPopupEventBus);
        setupColumns();
    }



    /*
     * Event Handlers
     */

    /**
     * UI Handler for the More Button
     *
     * @param event The event, triggered by a push on the More Button
     */
    @UiHandler("moreButton")
    void saveButtonPressed(ClickEvent event) {
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
        jobsTable.addColumn(constructJobStateColumn(), texts.columnHeader_JobStatus());
    }

    /**
     * This method constructs the JobCreationTime column
     *
     * @return the constructed JobCreationTime column
     */
    private Column constructJobCreationTimeColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getJobCreationTime();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return validateObjects(o1, o2) ? compareLongDates(o1.getJobCreationTime(), o2.getJobCreationTime()) : 1;
            }
        });
        column.setDefaultSortAscending(false);  // Set default sort order for jobCreationTime Column to Descending (youngest first)
        return column;
    }

    /**
     * This method constructs the JobId column
     *
     * @return the constructed JobId column
     */
    private Column constructJobIdColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getJobId();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return validateObjects(o1, o2) ? compareStringsAsLongs(o1.getJobId(), o2.getJobId()) : 1;
            }
        });
        return column;
    }

    /**
     * This method constructs the FileName column
     *
     * @return the constructed FileName column
     */
    private Column constructFileNameColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getFileName();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return validateObjects(o1, o2) ? compareStrings(o1.getFileName(), o2.getFileName()) : 1;
            }
        });
        return column;
    }

    /**
     * This method constructs the SubmitterNumber column
     *
     * @return the constructed SubmitterNumber column
     */
    private Column constructSubmitterNumberColumn() {
        TextColumn<JobModel> column = new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model.getSubmitterNumber();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<JobModel>() {
            public int compare(JobModel o1, JobModel o2) {
                return validateObjects(o1, o2) ? compareStringsAsLongs(o1.getSubmitterNumber(), o2.getSubmitterNumber()) : 1;
            }
        });
        return column;
    }

    /**
     * This method constructs the JobState column
     *
     * @return the constructed JobState column
     */
    private Column constructJobStateColumn() {
        final ImageResourceCell statusCell = new ImageResourceCell() {
            public Set<String> getConsumedEvents() {
                HashSet<String> events = new HashSet<String>();
                events.add("click");
                return events;
            }
        };
        return new StatusColumn(statusCell);
    }


    /**
     * Validates two objects. If any of the two objects are null pointers, the method returns false
     *
     * @param o1 The first object
     * @param o2 The second object
     * @return True if none of the two objects are null, false otherwise
     */
    private boolean validateObjects(Object o1, Object o2) {
        return o1 != null && o2 != null;
    }

    /**
     * Compares two strings as the numbers they represent
     *
     * @param s1 String containing first number
     * @param s2 String containing second number
     * @return 0 if equal, -1 if s1 is smaller than s2, +1 if s1 is greater than s2
     */
    private int compareStringsAsLongs(String s1, String s2) {
        long l1 = Long.parseLong(s1);
        long l2 = Long.parseLong(s2);
        return (l1 == l2) ? 0 : (l1 < l2) ? -1 : 1;
    }


    /**
     * Compares two strings as an alphanumeric ordering
     *
     * @param s1 String containing first string
     * @param s2 String containing second string
     * @return 0 if equal, -1 if s1 is smaller than s2, +1 if s1 is greater than s2
     */
    private int compareStrings(String s1, String s2) {
        return s1.compareTo(s2);
    }


    /**
     * Compares two dates (represented in strings)
     *
     * @param s1 String containing first date
     * @param s2 String containing second date
     * @return 0 if equal, -1 if s1 is smaller than s2, +1 if s1 is greater than s2
     */
    private int compareLongDates(String s1, String s2) {
        Long l1 = Format.parseLongDate(s1);
        Long l2 = Format.parseLongDate(s2);
        return l1.equals(l2) ? 0 : (l1 < l2) ? -1 : 1;
    }


    /**
     * This method maps the logical jobstatus to a JobStatusEnum
     *
     * @param model The JobInfo for the job to test
     * @return JobStatusEnum: NOT_DONE, DONE_WITHOUT_ERROR or DONE_WITH_ERROR
     */
    private JobStatus getJobStatus(JobModel model) {
        JobStatus jobStatus = JobStatus.DONE_WITHOUT_ERROR; // Default value
        if (model.getJobErrorCode() != JobErrorCode.NO_ERROR) {
            // The entire job has failed
            jobStatus = JobStatus.DONE_WITH_ERROR;
        } else {
            //Check if the job is completely done
            if (!model.getJobDone()) {
                jobStatus = JobStatus.NOT_DONE;
            } else if (model.getChunkifyingFailureCounter() > 0L
                    || model.getProcessingFailureCounter() > 0L
                    || model.getDeliveringFailureCounter() > 0L) {
                // Errors found
                jobStatus = JobStatus.DONE_WITH_ERROR;
            }
        }
        return jobStatus;
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


    /*
     * Private classes
     */

    /**
     * This class is a specialization of the Column class
     * It contains knowledge about the Status Popup window to be triggered by a click on the cell
     */
    class StatusColumn extends Column<JobModel, ImageResource> {
        /**
         * Default constructor
         *
         * @param cell The Image to put into the status column cell
         */
        public StatusColumn(Cell<ImageResource> cell) {
            super(cell);
        }

        /**
         * Event handler for handling browser events
         *
         * @param context The Cell.Context in which the event originates
         * @param parent  The element in which the event originates
         * @param model   The JobModel for the actual event
         * @param event   The event
         */
        @Override
        public void onBrowserEvent(Cell.Context context, Element parent, JobModel model, NativeEvent event) {
            super.onBrowserEvent(context, parent, model, event);
            if ("click".equals(event.getType())) {
                new StatusPopup(statusPopupEventBus, parent, model);
            }
        }

        /**
         * This method gets a style name, depending on a model
         *
         * @param context The Cell.Context in which the event originates
         * @param model   The model
         * @return The style name as a String
         */
        public String getCellStyleNames(Cell.Context context, JobModel model) {
            switch (getJobStatus(model)) {
                case NOT_DONE:
                    return GUICLASS_GRAY;
                case DONE_WITH_ERROR:
                    return GUICLASS_RED;
                default:
                    return GUICLASS_GREEN;
            }
        }

        /**
         * This method gets an image for a given model
         *
         * @param model The model
         * @return The image for the given model
         */
        @Override
        public ImageResource getValue(JobModel model) {
            switch (getJobStatus(model)) {
                case NOT_DONE:
                    return RESOURCES.gray();
                case DONE_WITH_ERROR:
                    return RESOURCES.red();
                default:
                    return RESOURCES.green();
            }
        }
    }


}
