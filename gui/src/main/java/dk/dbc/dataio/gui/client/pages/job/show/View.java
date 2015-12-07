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


/**
* This class is the View class for the New Jobs Show View
*/
public class View extends ViewWidget {
    protected static final int IS_FIXED_COLUMN = 1;
    protected static final int ASSIGNEE_COLUMN = 2;
    protected static final int ACTION_COLUMN = 3;

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
        jobsTable.addColumn(constructIsFixedColumn(), texts.columnHeader_Fixed());
        jobsTable.addColumn(constructAssigneeColumn(), texts.columnHeader_Assignee());
        jobsTable.addColumn(constructRerunColumn(), texts.columnHeader_Action());
        jobsTable.addColumn(constructJobCreationTimeColumn(), texts.columnHeader_JobCreationTime());
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
        Column<JobModel, Boolean> workflowNoteColumn = new Column<JobModel, Boolean>(checkboxCell) {
            @Override
            public Boolean getValue(JobModel jobModel) {
                if (jobModel.getWorkflowNoteModel() == null) {
                    return false;
                } else {
                    return jobModel.getWorkflowNoteModel().isProcessed();
                }
            }
        };
        return workflowNoteColumn;
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
        return new StatusColumn(commonInjector.getResources(), statusCell);
    }

    Column constructRerunColumn() {

        ButtonCell rerunButtonCell = new ButtonCell();
        Column<JobModel,String> rerunButtonColumn = new Column<JobModel,String>(rerunButtonCell) {
            public String getValue(JobModel object) {
                return getTexts().button_RerunJob();
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

    Texts getTexts() {
        return viewInjector.getTexts();

    }

    /*
     * Private methods
     */

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

    private String getHideShowSymbol() {
        return workFlowColumnsVisible ? "<" : ">";
    }

    private String getHideShowStyle() {
        return workFlowColumnsVisible ? "hide-cell" : "show-cell";
    }

    private void collapseColumn(boolean collapse, int columnIndex) {
        if (collapse) {
            jobsTable.addColumnStyleName(columnIndex, "collapsed");
        } else {
            jobsTable.removeColumnStyleName(columnIndex, "collapsed");
        }
    }

    private void HideColumn(boolean hide) {
        workFlowColumnsVisible = !hide;
        collapseColumn(hide, IS_FIXED_COLUMN);
        collapseColumn(hide, ASSIGNEE_COLUMN);
        collapseColumn(hide, ACTION_COLUMN);
        refreshJobsTable();
    }

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
            if(BrowserEvents.CLICK.equals(cellPreviewEvent.getNativeEvent().getType()) && cellPreviewEvent.getColumn() == IS_FIXED_COLUMN) {
                final WorkflowNoteModel workflowNoteModel = cellPreviewEvent.getValue().getWorkflowNoteModel();
                if(workflowNoteModel == null) {
                    Window.alert(getTexts().error_InputCellValidationError());
                    jobsTable.redraw();
                } else {
                    workflowNoteModel.setProcessed(workflowNoteModel.isProcessed() ? false : true);
                    presenter.setWorkflowNote(workflowNoteModel, selectionModel.getSelectedObject().getJobId());
                }
            }
        }
    }

    /**
     * Hide/Show Column Header class
     */
    class HideShowColumnHeader extends Header<String> {

        public HideShowColumnHeader(Cell cell) {
            super(cell);
        }

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
    };
}
