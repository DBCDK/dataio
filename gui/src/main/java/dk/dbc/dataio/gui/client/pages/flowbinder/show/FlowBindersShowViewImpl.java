package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.types.FlowBinderContentViewData;

import java.util.ArrayList;
import java.util.List;

/**
 * Show FlowBinderContentViewDatas view implementation
 Shows a table, containing:
  o Navn
  o Beskrivelse
  o RammeFormat
  o IndholdsFormat
  o Tegnsæt
  o Destination
  o Recordssplitter
  o Submittere
  o Flow
  o Sink
 */
public class FlowBindersShowViewImpl extends ContentPanel<FlowBindersShowPresenter> implements FlowBindersShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_FLOW_BINDERS_SHOW_WIDGET = "flowbindersshowwidget";
    public static final String CLASS_FLOW_BINDERS_SHOW_WIDGET_EDIT_BUTTON = "flowbindersshowwidget_editbutton";

    // Local variables
    private static final FlowBindersShowTexts constants = GWT.create(FlowBindersShowTexts.class);
    private final DioCellTable<FlowBinderContentViewData> table = new DioCellTable<FlowBinderContentViewData>();


    /**
     * Constructor
     */
    public FlowBindersShowViewImpl() {
        super(constants.menu_FlowBindersShow());
    }

    /**
     * Initializations of the view
     * Sets up the three columns in the CellTable
     */
    public void init() {
        table.updateStarted();

        getElement().setId(GUIID_FLOW_BINDERS_SHOW_WIDGET);

        if (table.getColumnCount() == 0) {
            // Name
            TextColumn<FlowBinderContentViewData> nameColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getContent().getName();
                }
            };
            table.addColumn(nameColumn, constants.columnHeader_Name());

            // Description
            TextColumn<FlowBinderContentViewData> descriptionColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getContent().getDescription();
                }
            };
            table.addColumn(descriptionColumn, constants.columnHeader_Description());

            // Packaging
            TextColumn<FlowBinderContentViewData> packagingColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getContent().getPackaging();
                }
            };
            table.addColumn(packagingColumn, constants.columnHeader_Packaging());

            // Format
            TextColumn<FlowBinderContentViewData> formatColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getContent().getFormat();
                }
            };
            table.addColumn(formatColumn, constants.columnHeader_Format());

            // Charset
            TextColumn<FlowBinderContentViewData> charsetColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getContent().getCharset();
                }
            };
            table.addColumn(charsetColumn, constants.columnHeader_Charset());

            // Destination
            TextColumn<FlowBinderContentViewData> destinationColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getContent().getDestination();
                }
            };
            table.addColumn(destinationColumn, constants.columnHeader_Destination());

            // RecordSplitter
            TextColumn<FlowBinderContentViewData> recordSplitterColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getContent().getRecordSplitter();
                }
            };
            table.addColumn(recordSplitterColumn, constants.columnHeader_RecordSplitter());

            // Submitters
            TextColumn<FlowBinderContentViewData> submittersColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    List<String> result = new ArrayList<String>();
                    for (SubmitterContent submitterContent: content.getSubmitters()) {
                        result.add(Format.inBracketsPairString(Long.toString(submitterContent.getNumber()), submitterContent.getName()));
                    }
                    return Format.commaSeparate(result);
                }
            };
            table.addColumn(submittersColumn, constants.columnHeader_Submitters());

            // Flow
            TextColumn<FlowBinderContentViewData> flowColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getFlowName();
                }
            };
            table.addColumn(flowColumn, constants.columnHeader_Flow());

            // Sink
            TextColumn<FlowBinderContentViewData> sinkColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getSinkName();
                }
            };
            table.addColumn(sinkColumn, constants.columnHeader_Sink());

            Column editButtonColumn =  new Column<FlowBinderContentViewData, String>(new ButtonCell()) {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    // The value to display in the button.
                    return constants.button_Update();
                }
            };

            //Define class name for the button element
            editButtonColumn.setCellStyleNames(CLASS_FLOW_BINDERS_SHOW_WIDGET_EDIT_BUTTON);

            // Handler: Registering key clicks (on the buttonCell available for each flow binder).
            // Clicks on ButtonCells are handled by setting the FieldUpdater for the Column
            editButtonColumn.setFieldUpdater(new FieldUpdater<FlowBinderContentViewData, String>() {
                @Override
                public void update(int index, FlowBinderContentViewData content, String buttonText) {
                    updateFlowBinder(content);
                }
            });

            table.addColumn(editButtonColumn, constants.columnHeader_Action_Update());
            add(table);
        }
    }


    /*
     * Implementation of interface methods
     */

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
    public void setStatusText(String message) {
    }

    /**
     * OnFailure
     * @param message The message to display to the user
     */
    @Override
    public void setErrorText(String message) {
        super.setErrorText(message);
        table.updateDone();
    }

    /**
     * setFlowBinderContentViewDatas is called by the presenter, to push table data to the view
     * @param flowBinders List of flows to view
     */
    @Override
    public void setFlowBinders(List<FlowBinderContentViewData> flowBinders) {
        table.setPageSize(flowBinders.size());
        table.setRowData(0, flowBinders);
        table.setRowCount(flowBinders.size());
        table.updateDone();
    }

    /**
     * When a key click has been registered, the updateFlowBinder method in FlowBindersShowPresenter is called,
     * in order to handle the update action itself.
     * @param content to display in the view
     */
    private void updateFlowBinder(FlowBinderContentViewData content){
        presenter.updateFlowBinder(content);
    }

}
