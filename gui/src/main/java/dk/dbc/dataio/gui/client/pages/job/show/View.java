package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.components.ClickableImageResourceCell;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.resources.Resources;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;

import java.util.ArrayList;
import java.util.List;


/**
 * This class is the View class for the New Jobs Show View
 */
public class View extends ViewWidget {
    // List of sinks that are known to be abortable
    ViewJobsGinjector viewInjector = GWT.create(ViewJobsGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private boolean dataHasNotYetBeenLoaded = true;
    private boolean workFlowColumnsVisible = true;
    private boolean assigneeFieldHasFocus = false;

    private int pageSize = 20;

    public AsyncJobViewDataProvider dataProvider;
    ProvidesKey<JobModel> keyProvider = jobModel -> (jobModel == null) ? null : jobModel.getJobId();
    SingleSelectionModel<JobModel> selectionModel = new SingleSelectionModel<>(keyProvider);
    JobRerunScheme jobRerunScheme;

    // Enums
    enum JobStatus {
        NOT_DONE, DONE_WITH_ERROR, DONE_WITHOUT_ERROR, PREVIEW, ABORTED;
    }

    public View() {
        this("", true);
        dataProvider.addDataDisplay(jobsTable);
        jobsTable.setVisibleRange(new Range(0, pageSize));
        HideColumn(true);  // Default: Do not show Work Flow columns
    }

    /* Package scoped Constructor used for unit testing. */
    View(String headerText, Boolean setupColumns) {
        super(headerText);
        if (setupColumns) {
            setupColumns();
        }
        dataProvider = new AsyncJobViewDataProvider(this, keyProvider);
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
            refreshJobsTable();
        }
    }

    /**
     * Sets the Page Size
     *
     * @param pageSize New page size
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        refreshJobsTable();
    }

    /**
     * Returns the information about whether the Assignee Field has focus
     *
     * @return True: Assignee field has focus, False: Assignee field has not focus
     */
    public boolean hasAssigneeFieldFocus() {
        return assigneeFieldHasFocus;
    }

    public void setPopupSelectBoxVisible() {
        popupSelectBox.setRightSelected(false);
        popupSelectBox.show();
    }


    /*
     * Implementation of abstract methods
     */

    /**
     * Reruns all shown jobs on the current page, excluding aged, outdated jobs.
     * This method is un-confirmed, meaning the purpose is to ask the user if a re-run is really wanted
     * If this is the case, call the confirmed rerunAllShownJobs method (rerunAllShownJobsConfirmed)
     */
    void rerunAllShownJobs() {
        List<JobModel> reRunnableJobModels = presenter.validRerunJobsFilter(presenter.getShownJobModels());
        List<String> reRunnable = new ArrayList<>();
        for (JobModel jobModel : reRunnableJobModels) {
            reRunnable.add(jobModel.getJobId());
        }

        int count = reRunnable.size();
        switch (count) {
            case 0:
                setDialogTexts("", getTexts().error_NoJobsToRerun(), "");
                rerunOkButton.setVisible(false);
                break;
            case 1:
                setDialogTexts(getTexts().label_RerunJob(), Format.commaSeparate(reRunnable), getTexts().label_RerunJobConfirmation());
                rerunOkButton.setVisible(true);
                break;
            default:  // count > 1
                setDialogTexts(
                        Format.macro(getTexts().label_RerunJobs(), "COUNT", String.valueOf(count)),
                        Format.commaSeparate(getShownJobIds()),
                        getTexts().label_RerunJobsConfirmation()
                );
                rerunOkButton.setVisible(true);
                break;
        }
        rerunAllShownJobsConfirmationDialog.show();
    }

    /**
     * Reruns all shown jobs on the current page (now the user has confirmed the action)
     */
    void rerunAllShownJobsConfirmed() {
        presenter.setIsMultipleRerun(true);
        setPopupSelectBoxVisible();
    }

    /**
     * Sets the autorefresh feature in the Data View Provider
     *
     * @param autoRefresh If true, auto refresh should be enabled, otherwise it should be disabled.
     */
    void setAutoRefresh(boolean autoRefresh) {
        dataProvider.setAutoRefresh(autoRefresh);
    }


    /*
     * Private methods
     */

    /**
     * Sets the three texts in the Confirmation Dialog Box
     *
     * @param count        The text to set for the jobs counter
     * @param list         The text to set for the jobs list
     * @param confirmation The text to set for the confirmation text
     */
    private void setDialogTexts(String count, String list, String confirmation) {
        rerunJobsCount.setText(count);
        rerunJobsList.setText(list);
        rerunJobsConfirmation.setText(confirmation);
    }


    /**
     * Finds all job id's from the shown jobs in the jobs table
     *
     * @return List of Job Ids
     */
    private List<String> getShownJobIds() {
        List<String> jobIds = new ArrayList<>();
        for (JobModel model : presenter.validRerunJobsFilter(presenter.getShownJobModels())) {
            jobIds.add(model.getJobId());
        }
        return jobIds;
    }

    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setJobs method
     */
    @SuppressWarnings("unchecked")
    void setupColumns() {
        jobsTable.addColumn(constructHideShowWorkflow(), new HideShowColumnHeader());
        jobsTable.addColumn(constructIsFixedColumn(), new HidableColumnHeader(getTexts().columnHeader_Fixed()));
        jobsTable.addColumn(constructAssigneeColumn(), new HidableColumnHeader(getTexts().columnHeader_Assignee()));
        jobsTable.addColumn(constructRerunColumn(), new HidableColumnHeader(getTexts().columnHeader_Action()));
        jobsTable.addColumn(constructAbortColumn(), new HidableColumnHeader(getTexts().columnHeader_Action()));
        jobsTable.addColumn(constructJobCreationTimeColumn(), getTexts().columnHeader_JobCreationTime());
        jobsTable.addColumn(constructJobIdColumn(), getTexts().columnHeader_JobId());
        jobsTable.addColumn(constructSubmitterColumn(), getTexts().columnHeader_Submitter());
        jobsTable.addColumn(constructFlowBinderNameColumn(), getTexts().columnHeader_FlowBinderName());
        jobsTable.addColumn(constructSinkNameColumn(), getTexts().columnHeader_SinkName());
        jobsTable.addColumn(constructItemCountColumn(), getTexts().columnHeader_TotalChunkCount());
        jobsTable.addColumn(constructFailedCounterColumn(), getTexts().columnHeader_FailureCounter());
        jobsTable.addColumn(constructIgnoredCounterColumn(), getTexts().columnHeader_IgnoredCounter());
        jobsTable.addColumn(constructProgressBarColumn(), getTexts().columnHeader_ProgressBar());
        jobsTable.addColumn(constructJobStateColumn(), getTexts().columnHeader_JobStatus());
        jobsTable.setSelectionModel(selectionModel);
        jobsTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());

        pagerTop.setDisplay(jobsTable);
        pagerBottom.setDisplay(jobsTable);
    }

    /**
     * This method constructs the Hide/Show Workflow column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Hide/Show Workflow column
     */
    Column constructHideShowWorkflow() {
        return new HideShowCell(
                commonInjector.getResources(),
                new ClickableImageResourceCell() {
                    @Override
                    public void render(Cell.Context context, ImageResource value, final SafeHtmlBuilder sb) {
                        int previousId = 0;
                        JobModel jobModel = (JobModel) context.getKey();
                        if (jobModel != null) {
                            previousId = jobModel.getPreviousJobIdAncestry();
                        }
                        if (previousId != 0) {
                            sb.append(SafeHtmlUtils.fromSafeConstant("<span title='" + getTexts().label_RerunJobNo() + " " + previousId + "'>"));
                        }
                        sb.append(renderer.render(value));
                        if (previousId != 0) {
                            sb.append(SafeHtmlUtils.fromSafeConstant("</span>"));
                        }
                    }
                }
        );
    }

    /**
     * This method constructs the IsFixed column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed IsFixed column
     */
    Column constructIsFixedColumn() {
        CheckboxCell checkboxCell = new CheckboxCell(true, false);
        return new Column<JobModel, Boolean>(checkboxCell) {
            @Override
            public Boolean getValue(JobModel jobModel) {
                return jobModel != null && jobModel.getWorkflowNoteModel() != null && jobModel.getWorkflowNoteModel().isProcessed();
            }

            @Override
            public String getCellStyleNames(Cell.Context context, JobModel model) {
                return workFlowColumnsVisible ? "visible" : "invisible";
            }

            @Override
            public void onBrowserEvent(Cell.Context context, Element elem, JobModel jobModel, NativeEvent event) {
                if (Event.as(event).getTypeInt() == Event.ONCHANGE) {
                    final WorkflowNoteModel workflowNoteModel = jobModel.getWorkflowNoteModel();
                    if (workflowNoteModel.getAssignee().isEmpty()) {
                        Window.alert(getTexts().error_InputCellValidationError());
                        jobsTable.redraw();
                    } else {
                        workflowNoteModel.setProcessed(((InputElement) elem.getFirstChild()).isChecked());
                        presenter.setWorkflowNote(workflowNoteModel, jobModel.getJobId());
                    }
                }
                super.onBrowserEvent(context, elem, jobModel, event);
            }
        };
    }

    /**
     * This method constructs the Assignee column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Assignee column
     */
    Column constructAssigneeColumn() {
        Column<JobModel, String> assigneeColumn = new Column<JobModel, String>(new TextInputCell()) {
            @Override
            public void onBrowserEvent(Cell.Context context, Element elem, JobModel object, NativeEvent event) {
                switch (event.getType()) {
                    case "focus":
                        assigneeFieldHasFocus = true;
                        break;
                    case "blur":
                        assigneeFieldHasFocus = false;
                        break;
                    default:
                        break;
                }
                super.onBrowserEvent(context, elem, object, event);
            }

            @Override
            public String getValue(JobModel model) {
                return model != null && model.getWorkflowNoteModel() != null ? model.getWorkflowNoteModel().getAssignee() : null;
            }

            @Override
            public String getCellStyleNames(Cell.Context context, JobModel model) {
                if (model != null && model.getWorkflowNoteModel() != null && !model.getWorkflowNoteModel().getDescription().isEmpty()) {
                    return workFlowColumnsVisible ? "tooltip visible" : "tooltip invisible";
                } else {
                    return workFlowColumnsVisible ? "visible" : "invisible";
                }
            }

            @Override
            public void render(Cell.Context context, JobModel jobModel, SafeHtmlBuilder sb) {
                if (jobModel != null) {
                    String note = jobModel.getWorkflowNoteModel() == null ? "" : jobModel.getWorkflowNoteModel().getDescription();
                    super.render(context, jobModel, sb);
                    if (!note.isEmpty()) {
                        sb.append(SafeHtmlUtils.fromSafeConstant("<span class='tooltiptext'>" + note + "</span>"));
                    }
                }
            }
        };
        assigneeColumn.setFieldUpdater((index, selectedRowModel, value) -> {
            if (value != null) {
                WorkflowNoteModel selectedWorkflowNoteModel = selectedRowModel.getWorkflowNoteModel();
                if (!value.isEmpty() || !selectedWorkflowNoteModel.getAssignee().isEmpty() || !selectedWorkflowNoteModel.getDescription().isEmpty()) {
                    WorkflowNoteModel updatedWorkflowNoteModel = presenter.preProcessAssignee(selectedWorkflowNoteModel, value);
                    if (updatedWorkflowNoteModel == null) {
                        refreshJobsTable();
                    } else {
                        presenter.setWorkflowNote(updatedWorkflowNoteModel, selectedRowModel.getJobId());
                        selectedRowModel.withWorkflowNoteModel(updatedWorkflowNoteModel);
                    }
                }
            }
            dataProvider.refresh();
        });
        return assigneeColumn;
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
                return model == null ? "" : model.getJobCreationTime();
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
                return model == null ? "" : model.getJobId();
            }
        };
    }

    /**
     * This method constructs the Submitter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Submitter column
     */
    Column constructSubmitterColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return model == null ? "" : Format.inBracketsPairString(model.getSubmitterNumber(), model.getSubmitterName());
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
                return model == null ? "" : model.getFlowBinderName();
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
                return model == null ? "" : model.getSinkName();
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
                return model == null ? "" : String.valueOf(model.getNumberOfItems());
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
                return model == null ? "" : String.valueOf(model.getStateModel().getFailedCounter());
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
                return model == null ? "" : String.valueOf(model.getStateModel().getProcessing().getFailed());
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
        return new StatusColumn(commonInjector.getResources(), statusCell);
    }

    /**
     * This method constructs the ReRun column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed ReRun column
     */
    Column constructRerunColumn() {
        ButtonCell rerunButtonCell = new ButtonCell();
        Column<JobModel, String> rerunButtonColumn = new Column<>(rerunButtonCell) {
            public String getValue(JobModel object) {
                return object != null && object.getJobCompletionTime().isEmpty() ? getTexts().button_ResendJob() : getTexts().button_RerunJob();
            }

            @Override
            public String getCellStyleNames(Cell.Context context, JobModel model) {
                return workFlowColumnsVisible ? "visible" : "invisible";
            }
        };
        rerunButtonColumn.setFieldUpdater((index, selectedRowModel, value) -> {
            if (selectedRowModel != null) {
                presenter.setIsMultipleRerun(false);
                if (selectedRowModel.getJobCompletionTime().isEmpty()) {
                    presenter.resendJob(selectedRowModel);
                } else {

                    presenter.getJobRerunScheme(selectedRowModel);
                }
            }
        });
        return rerunButtonColumn;
    }

    Column constructAbortColumn() {
        ButtonCell abortButtonCell = new ButtonCell();
        Column<JobModel, String> abortButtonColumn = new Column<>(abortButtonCell) {
            public String getValue(JobModel object) {
                return getTexts().button_AbortJob();
            }

            @Override
            public void render(Cell.Context context, JobModel object, SafeHtmlBuilder sb) {
                if(object.getJobCompletionTime().isEmpty()) {
                    super.render(context, object, sb);
                }
            }

            @Override
            public String getCellStyleNames(Cell.Context context, JobModel model) {
                return workFlowColumnsVisible ? "visible" : "invisible";
            }
        };
        abortButtonColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        abortButtonColumn.setFieldUpdater((index, selectedRowModel, value) -> {
            if (selectedRowModel != null) {
                if (selectedRowModel.getJobCompletionTime().isEmpty()) {
                    presenter.setIsMultipleRerun(false);
                    presenter.abortJob(selectedRowModel);
                } else {
                    setErrorText(getTexts().error_JobFinishedError());
                }
            }
        });
        return abortButtonColumn;
    }

    /*
     * Private methods
     */

    /**
     * This method fetches the texts attribute
     *
     * @return The texts for this class
     */
    Texts getTexts() {
        return viewInjector.getTexts();
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> {
            JobModel selected = selectionModel.getSelectedObject();
            if (selected != null) {
                presenter.itemSelected(selected);
            }
        };
    }

    /**
     * Fetches the Hide/Show symbol to be shown as the header text for the hide/show column
     *
     * @return The Hide/Show symbol
     */
    private String getHideShowSymbol() {
        return workFlowColumnsVisible ? "<" : ">";
    }

    /**
     * Fetches the css class to be used for a cell
     *
     * @return The CSS class name for a cell
     */
    private String getHideShowStyle() {
        return workFlowColumnsVisible ? "hide-cell" : "show-cell";
    }

    /**
     * Hides or Shows a hideable column
     *
     * @param hide Boolean stating whether to show or hide a column: True=hide, False=show
     */
    void HideColumn(boolean hide) { // Should have been private, but is package-private to enable unit test
        workFlowColumnsVisible = !hide;
        refreshJobsTable();
    }

    /**
     * Toggles the visibility of a hideable column
     */
    private void HideOrShowColumn() {
        HideColumn(workFlowColumnsVisible);
    }


    /*
     * Local classes
     * These classes should all be private, but in order to enable unit test, they are package scoped
     */

    /**
     * Normal Column Header class (to be hidden upon request)
     */
    class HidableColumnHeader extends Header<String> {
        private String headerText;

        public HidableColumnHeader(String text) {
            super(new TextCell());
            headerText = text;
        }

        @Override
        public String getValue() {
            return headerText;
        }

        @Override
        public String getHeaderStyleNames() {
            return workFlowColumnsVisible ? "visible" : "invisible";
        }
    }

    /**
     * Hide/Show Column Header class
     */
    class HideShowColumnHeader extends Header<String> {
        public HideShowColumnHeader() {
            super(new ClickableTextCell());
        }

        @Override
        public String getValue() {
            return getHideShowSymbol();
        }

        @Override
        public String getHeaderStyleNames() {
            return getHideShowStyle();
        }

        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, NativeEvent event) {
            super.onBrowserEvent(context, elem, event);
            if ("click".equals(event.getType())) {
                HideOrShowColumn();
            }
        }
    }

    /**
     * Hide/Show Cell class
     */
    class HideShowCell extends Column<JobModel, ImageResource> {
        private final Resources resources;

        public HideShowCell(Resources resources, Cell<ImageResource> cell) {
            super(cell);
            this.resources = resources;
        }

        @Override
        public ImageResource getValue(JobModel model) {
            if (model != null && model.getPreviousJobIdAncestry() != 0) {
                return resources.recycleIcon();
            } else {
                return resources.emptyIcon();
            }
        }

        @Override
        public String getCellStyleNames(Cell.Context context, JobModel model) {
            return getHideShowStyle();
        }

        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, JobModel model, NativeEvent event) {
            super.onBrowserEvent(context, elem, model, event);
            if ("click".equals(event.getType())) {
                HideOrShowColumn();
            }
        }
    }


}
