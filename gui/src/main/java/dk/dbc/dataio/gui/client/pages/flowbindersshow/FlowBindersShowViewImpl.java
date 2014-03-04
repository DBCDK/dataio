package dk.dbc.dataio.gui.client.pages.flowbindersshow;

import com.google.gwt.core.client.GWT;
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

    // Local variables
    private static final FlowBindersShowConstants constants = GWT.create(FlowBindersShowConstants.class);
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
                    return content.getName();
                }
            };
            table.addColumn(nameColumn, constants.columnHeader_Name());

            // Description
            TextColumn<FlowBinderContentViewData> descriptionColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getDescription();
                }
            };
            table.addColumn(descriptionColumn, constants.columnHeader_Description());

            // Packaging
            TextColumn<FlowBinderContentViewData> packagingColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getPackaging();
                }
            };
            table.addColumn(packagingColumn, constants.columnHeader_Packaging());

            // Format
            TextColumn<FlowBinderContentViewData> formatColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getFormat();
                }
            };
            table.addColumn(formatColumn, constants.columnHeader_Format());

            // Charset
            TextColumn<FlowBinderContentViewData> charsetColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getCharset();
                }
            };
            table.addColumn(charsetColumn, constants.columnHeader_Charset());

            // Destination
            TextColumn<FlowBinderContentViewData> destinationColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getDestination();
                }
            };
            table.addColumn(destinationColumn, constants.columnHeader_Destination());

            // RecordSplitter
            TextColumn<FlowBinderContentViewData> recordSplitterColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    return content.getRecordSplitter();
                }
            };
            table.addColumn(recordSplitterColumn, constants.columnHeader_RecordSplitter());

            // Submitters
            TextColumn<FlowBinderContentViewData> submittersColumn = new TextColumn<FlowBinderContentViewData>() {
                @Override
                public String getValue(FlowBinderContentViewData content) {
                    List<String> result = new ArrayList<String>();
                    for (SubmitterContent submitterContent: content.getSubmitters()) {
                        result.add(Format.submitterPairString(submitterContent.getNumber(), submitterContent.getName()));
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

            add(table);
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
     * setFlowBinderContentViewDatas is called by the presenter, to push table data to the view
     * @param flowBinders List of flows to view
     */
    @Override
    public void setFlowBinders(List<FlowBinderContentViewData> flowBinders) {
        table.setRowData(0, flowBinders);
        table.setRowCount(flowBinders.size());
        table.updateDone();
    }

}
