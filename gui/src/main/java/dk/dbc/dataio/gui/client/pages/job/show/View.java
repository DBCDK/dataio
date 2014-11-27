package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.commons.types.JobErrorCode;
import dk.dbc.dataio.gui.client.components.DualPanesPanel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.resource.Resources;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class is the View class for the Jobs Show View
 */
public class View extends ContentPanel<Presenter> implements IsWidget {
    private static final String GUICLASS_GRAY = "gray-lamp";
    private static final String GUICLASS_GREEN = "green-lamp";
    private static final String GUICLASS_RED = "red-lamp";
    private static final int POPUP_PANEL_WIDTH = 265;
    private static final int POPUP_PANEL_LEFT_OFFSET = 36;
    private static final int POPUP_PANEL_TOP_OFFSET = 18;

    public static final String GUIID_JOBS_STATUS_DUAL_PANES_PANEL = "jobsstatusdualpanespanel";

    private Texts texts;
    private static final Resources RESOURCES = GWT.create(Resources.class);
    String jobStoreFilesystemUrl = "";
    private int currentPageSize = PAGE_SIZE;
    ColumnSortEvent.ListHandler<JobModel> columnSortHandler;

    // Configuration constants
    private static final int PAGE_SIZE = 20;

    // Enums
    private enum JobStatus {NOT_DONE, DONE_WITH_ERROR, DONE_WITHOUT_ERROR}

    // UI Fields
    @UiField CellTable jobsTable;
    @UiField Button moreButton;

    private Column jobCreationTimeColumn;

    PopupPanel popupPanel;


    @Override
    public void init() {}

    interface ViewUiBinder extends UiBinder<Widget, View> {}
    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    private ListDataProvider<JobModel> dataProvider;


    /**
     * Default constructor
     * @param header The header text for the View
     * @param texts The I8n texts for this view
     */
    public View(String header, Texts texts) {
        super(header);
        this.texts = texts;
        add(uiBinder.createAndBindUi(this));
        setupColumns();
    }


    /**
     * UI Handler for the More Button
     * @param event The event, triggered by a push on the More Button
     */
    @UiHandler("moreButton")
    void saveButtonPressed(ClickEvent event) {
        increasePageSize();
    }


    /**
     * This method is triggered, when the view is being unloaded, and causes the popup window to disappear
     */
    @Override
    protected void onUnload() {
        popupPanel.hide();
    }


    /**
     * This method is called by the presenter, to inject the jobStoreFilesystemUrl to be used by the view
     *
     * @param jobStoreFilesystemUrl JobStore FileSystem Url to be used by the View
     */
    public void setJobStoreFilesystemUrl(String jobStoreFilesystemUrl) {
        if (jobStoreFilesystemUrl != null) {
            this.jobStoreFilesystemUrl = jobStoreFilesystemUrl;
        }
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
        return new Column<JobModel, ImageResource>(statusCell) {
            @Override
            public void onBrowserEvent(Cell.Context context, Element elem,
                                       JobModel model, NativeEvent event) {
                super.onBrowserEvent(context, elem, model, event);
                if ("click".equals(event.getType())) {

                    // Create a basic popup widget
                    popupPanel = new PopupPanel(true);
                    int left = elem.getAbsoluteRight() - POPUP_PANEL_WIDTH - POPUP_PANEL_LEFT_OFFSET;
                    int top = elem.getAbsoluteTop() + POPUP_PANEL_TOP_OFFSET;
                    popupPanel.setPopupPosition(left, top);
                    popupPanel.setWidth(POPUP_PANEL_WIDTH + "px");
                    popupPanel.setWidget(buildFlowPanelForPopupPanel(model));
                    popupPanel.show();
                }
            }
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
        };
    }


    /**
     * This method builds a new panel, holding the Popup, to be triggered, when pushing the Status Icon
     * @param model The model associated with the Popup panel
     * @return The Popup Panel
     */
    private FlowPanel buildFlowPanelForPopupPanel(final JobModel model) {
        FlowPanel panel = new FlowPanel();
        DualPanesPanel dualPanesPanel = new DualPanesPanel(GUIID_JOBS_STATUS_DUAL_PANES_PANEL);

        if (model.getJobErrorCode().equals(JobErrorCode.NO_ERROR)) {
            panel.add(buildDualPanesPanelForChunkCounter(texts.text_chunkifying(), model.getChunkifyingFailureCounter()));
            panel.add(buildDualPanesPanelForChunkCounter(texts.text_processing(), model.getProcessingFailureCounter()));
            panel.add(buildDualPanesPanelForChunkCounter(texts.text_delivering(), model.getDeliveringFailureCounter()));
        } else {
            dualPanesPanel.setDualPanesPanelWidgets(getRedImageWithId(), new Label(model.getJobErrorCode().toString()));
            panel.add(dualPanesPanel);
        }
        panel.add(new Anchor(texts.link_MoreInfo(), getJobstoreLink(model.getJobId()), "_blank"));
        panel.add(new FlowPanel());  // Line break
        panel.add(getFailedItemsAnchor(model));
        return panel;
    }


    /**
     * This method builds a single panel, containing info about an operational state
     * @param operationalState The operational state to show info for
     * @param failureCounter Number of fails for the operational state
     * @return The pane, containing the info
     */
    private DualPanesPanel buildDualPanesPanelForChunkCounter(String operationalState, long failureCounter) {
        DualPanesPanel dualPanesPanel = new DualPanesPanel(GUIID_JOBS_STATUS_DUAL_PANES_PANEL);

        if (failureCounter == 0) {
            dualPanesPanel.setDualPanesPanelWidgets(getGreenImageWithId(), new Label(operationalState + " : " + texts.text_done()));

        } else if (failureCounter > 0) {
            String format = failureCounter == 1 ? " " + texts.text_record() + " " : " " + texts.text_records() + " ";
            dualPanesPanel.setDualPanesPanelWidgets(getRedImageWithId(),
                    new Label(operationalState + " : " + failureCounter + format + texts.text_failed()));
        } else {
            dualPanesPanel.setDualPanesPanelWidgets(getGreyImageWithId(), new Label(operationalState + " : " + texts.text_pending()));
        }
        return dualPanesPanel;
    }


    /**
     * This method constructs an anchor, that points to the Failed Items list for the model, passed as a parameter
     * @param model The model, to construct an anchor for
     * @return The anchor, pointing to the Failed Items list
     */
    private Anchor getFailedItemsAnchor(final JobModel model) {
        Anchor failedItemsAnchor = new Anchor(texts.link_FailedItems());
        failedItemsAnchor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                presenter.showFailedItems(model.getJobId());
            }
        });
        return failedItemsAnchor;
    }


    /**
     * This method gets a grey Image object
     * @return The grey Image object
     */
    private Image getGreyImageWithId() {
        Image image = new Image(RESOURCES.gray());
        image.getElement().setId(GUICLASS_GRAY);
        return image;
    }


    /**
     * This method gets a red Image object
     * @return The red Image object
     */
    private Image getRedImageWithId() {
        Image image = new Image(RESOURCES.red());
        image.getElement().setId(GUICLASS_RED);
        return image;
    }


    /**
     * This method gets a green Image object
     * @return The green Image object
     */
    private Image getGreenImageWithId() {
        Image image = new Image(RESOURCES.green());
        image.getElement().setId(GUICLASS_GREEN);
        return image;
    }


    /**
     * This method gets the link to the Job Store for a given id
     * @param id The identification of the data
     * @return The link (as a string url) to the given Job Store
     */
    private String getJobstoreLink(String id) {
        return jobStoreFilesystemUrl.concat("/").concat(id);
    }


    /**
     * Validates two objects. If any of the two objects are null pointers, the method returns false
     * @param o1 The first object
     * @param o2 The second object
     * @return True if none of the two objects are null, false otherwise
     */
    private boolean validateObjects(Object o1, Object o2) {
        return o1 != null && o2 != null;
    }


    /**
     * Compares two strings as the numbers they represent
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
     * @param s1 String containing first string
     * @param s2 String containing second string
     * @return 0 if equal, -1 if s1 is smaller than s2, +1 if s1 is greater than s2
     */
    private int compareStrings(String s1, String s2) {
        return s1.compareTo(s2);
    }


    /**
     * Compares two dates (represented in strings)
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
     * Public methods
     */

    /**
     * This method is used to put data into the view
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


}
