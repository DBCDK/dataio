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

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.model.WorkflowNoteModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;


/**
* This class is the View class for the New Jobs Show View
*/
public class View extends ViewWidget {
    protected static final int IS_FIXED_COLUMN = 1;
    protected static final int ASSIGNEE_COLUMN = 2;

    ViewJobsGinjector viewInjector = GWT.create(ViewJobsGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private boolean dataHasNotYetBeenLoaded = true;
    private boolean workFlowColumnsVisible = true;

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

    public View() {
        this("", true, true);
        dataProvider.addDataDisplay(jobsTable);
        HideColumn(true);  // Default: Do not show Work Flow columns
    }

    /* Package scoped Constructor used for unit testing. */
    View(String headerText, Boolean setupColumns, Boolean updateUserCriteria) {
        super(headerText);
        if( setupColumns ) {
            setupColumns();
        }
        dataProvider = new AsyncJobViewDataProvider(this, updateUserCriteria);
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
        Texts texts = getTexts();
        jobsTable.addColumn(constructHideShowWorkflow(), new HideShowColumnHeader());
        jobsTable.addColumn(constructIsFixedColumn(), new HidableColumnHeader(texts.columnHeader_Fixed()));
        jobsTable.addColumn(constructAssigneeColumn(), new HidableColumnHeader(texts.columnHeader_Assignee()));
        jobsTable.addColumn(constructRerunColumn(), new HidableColumnHeader(texts.columnHeader_Action()));
        jobsTable.addColumn(constructJobCreationTimeColumn(), texts.columnHeader_JobCreationTime());
        jobsTable.addColumn(constructJobIdColumn(), texts.columnHeader_JobId());
        jobsTable.addColumn(constructSubmitterColumn(), texts.columnHeader_Submitter());
        jobsTable.addColumn(constructFlowBinderNameColumn(), texts.columnHeader_FlowBinderName());
        jobsTable.addColumn(constructSinkNameColumn(), texts.columnHeader_SinkName());
        jobsTable.addColumn(constructItemCountColumn(), texts.columnHeader_TotalChunkCount());
        jobsTable.addColumn(constructFailedCounterColumn(), texts.columnHeader_FailureCounter());
        jobsTable.addColumn(constructIgnoredCounterColumn(), texts.columnHeader_IgnoredCounter());
        jobsTable.addColumn(constructProgressBarColumn(), texts.columnHeader_ProgressBar());
        jobsTable.addColumn(constructJobStateColumn(), texts.columnHeader_JobStatus());
        jobsTable.setSelectionModel(selectionModel);
        jobsTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
        jobsTable.addCellPreviewHandler(new CellPreviewHandlerClass());

        pagerTop.setDisplay(jobsTable);
        pagerBottom.setDisplay(jobsTable);

        jobsTable.setVisibleRange(0,20);
    }

    /**
     * This method constructs the Hide/Show Workflow column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Hide/Show Workflow column
     */
    Column constructHideShowWorkflow() {
        return new HideShowCell();
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
                return jobModel.getWorkflowNoteModel() != null && jobModel.getWorkflowNoteModel().isProcessed();
            }
            @Override
            public String getCellStyleNames(Cell.Context context, JobModel model) {
                return workFlowColumnsVisible ? "visible" : "invisible";
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
        TextInputCell textInputCell = new TextInputCell();
        final Cell.Context[] currentContext = new Cell.Context[1];
        Column<JobModel, String> assigneeColumn = new Column<JobModel, String>(textInputCell)
        {
            @Override
            public String getValue(JobModel model) {
                return model.getWorkflowNoteModel() != null ? model.getWorkflowNoteModel().getAssignee() : null;
            }

            @Override
            public void onBrowserEvent(Cell.Context context, Element elem, JobModel jobModel, NativeEvent event) {
                if(event.getType().equals(BrowserEvents.CHANGE) || event.getKeyCode() == KeyCodes.KEY_ENTER) {
                    currentContext[0] = context;
                    selectionModel.setSelected(jobModel, true);
                }
                super.onBrowserEvent(context, elem, jobModel, event);
            }
            @Override
            public String getCellStyleNames(Cell.Context context, JobModel model) {
                return workFlowColumnsVisible ? "visible" : "invisible";
            }
        };

        assigneeColumn.setFieldUpdater(new FieldUpdater<JobModel, String>() {
            @Override
            public void update(int index, JobModel selectedRowModel, String value) {
                WorkflowNoteModel updatedWorkflowNoteModel = presenter.preProcessAssignee(value);
                if(updatedWorkflowNoteModel != null) {
                    presenter.setWorkflowNote(updatedWorkflowNoteModel, selectedRowModel.getJobId());

                    // Update the TextInputCell value after save in order to display assignee with capital letters
                    // without reloading all table data.
                    TextInputCell.ViewData updatedViewData = new TextInputCell.ViewData(updatedWorkflowNoteModel.getAssignee());
                    TextInputCell updatedTextInputCell = (TextInputCell) jobsTable.getColumn(ASSIGNEE_COLUMN).getCell();
                    updatedTextInputCell.setViewData(currentContext[0].getKey(), updatedViewData);
                    selectedRowModel.setWorkflowNoteModel(updatedWorkflowNoteModel);
                    jobsTable.redraw();
                }
            }
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
     * This method constructs the Submitter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Submitter column
     */
    Column constructSubmitterColumn() {
        return new TextColumn<JobModel>() {
            @Override
            public String getValue(JobModel model) {
                return Format.inBracketsPairString(model.getSubmitterNumber(), model.getSubmitterName());
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
        Column<JobModel,String> rerunButtonColumn = new Column<JobModel,String>(rerunButtonCell) {
            public String getValue(JobModel object) {
                return getTexts().button_RerunJob();
            }
            @Override
            public String getCellStyleNames(Cell.Context context, JobModel model) {
                return workFlowColumnsVisible ? "visible" : "invisible";
            }
        };
        rerunButtonColumn.setFieldUpdater(new FieldUpdater<JobModel, String>() {
            @Override
            public void update(int index, JobModel selectedRowModel, String value) {
                if(selectedRowModel != null) {
                    presenter.editJob(selectedRowModel);
                }
            }
        });
        return rerunButtonColumn;
    }

    /*
     * Private methods
     */

    /**
     * This method fetches the texts attribute
     * @return The texts for this class
     */
    Texts getTexts() {
        return viewInjector.getTexts();
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler(){
        return new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent doubleClickEvent) {
                JobModel selected = selectionModel.getSelectedObject();
                if(selected != null) {
                    presenter.itemSelected(selected);
                }
            }
        };
    }

    /**
     * Fetches the Hide/Show symbol to be shown as the header text for the hide/show column
     * @return The Hide/Show symbol
     */
    private String getHideShowSymbol() {
        return workFlowColumnsVisible ? "<" : ">";
    }

    /**
     * Fetches the css class to be used for a cell
     * @return The CSS class name for a cell
     */
    private String getHideShowStyle() {
        return workFlowColumnsVisible ? "hide-cell" : "show-cell";
    }

    /**
     * Hides or Shows a hideable column
     * @param hide Boolean stating whether to show or hide a column: True=hide, False=show
     */
    void HideColumn(boolean hide) { // Should have been private, but is package-private to enable unit test
        workFlowColumnsVisible = !hide;
        refreshJobsTable();
    }

    /**
     * Toggles the visiblity of a hideable column
     */
    private void HideOrShowColumn() {
        HideColumn(workFlowColumnsVisible);
    }


    /*
     * Local classes
     * These classes should all be private, but in order to enable unit test, they are package scoped
     */

    /**
     * Cell Preview Handler
     */
    class CellPreviewHandlerClass implements CellPreviewEvent.Handler<JobModel> {
        @Override
        public void onCellPreview(CellPreviewEvent<JobModel> cellPreviewEvent) {
            if (cellPreviewEvent != null
                    && BrowserEvents.CLICK.equals(cellPreviewEvent.getNativeEvent().getType())
                    && cellPreviewEvent.getColumn() == IS_FIXED_COLUMN) {
                final WorkflowNoteModel workflowNoteModel = cellPreviewEvent.getValue().getWorkflowNoteModel();
                if(workflowNoteModel == null) {
                    Window.alert(getTexts().error_InputCellValidationError());
                    jobsTable.redraw();
                } else {
                    workflowNoteModel.setProcessed(!workflowNoteModel.isProcessed());
                    presenter.setWorkflowNote(workflowNoteModel, selectionModel.getSelectedObject().getJobId());
                }
            }
        }
    }

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
    class HideShowCell extends Column<JobModel, String> {
        public HideShowCell(Cell<String> cell) {
            super(cell);
        }

        public HideShowCell() {
            this(new ClickableTextCell());
        }

        @Override
        public String getValue(JobModel model) {
            return "";
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
