package dk.dbc.dataio.gui.client.pages.flowsshow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Show Flows view implementation
 * Shows a table, containing:
 *  o Name
 *  o Description
 *  o List of Flowcomponents, which are the Flowcomponent names separated by commas
 */
public class FlowsShowViewImpl extends ContentPanel<FlowsShowPresenter> implements FlowsShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_FLOWS_SHOW_WIDGET = "flowsshowwidget";

    // Local variables
    private static final FlowsShowConstants constants = GWT.create(FlowsShowConstants.class);
    private final DioCellTable<Flow> table = new DioCellTable<Flow>();


    /**
     * Constructor
     */
    public FlowsShowViewImpl() {
        super(constants.menu_Flows());
    }

    /**
     * Initializations of the view
     * Sets up the three columns in the CellTable
     */
    public void init() {
        table.updateStarted();

        getElement().setId(GUIID_FLOWS_SHOW_WIDGET);

        if (table.getColumnCount() == 0) {
            TextColumn<Flow> nameColumn = new TextColumn<Flow>() {
                @Override
                public String getValue(Flow content) {
                    return content.getContent().getName();
                }
            };
            table.addColumn(nameColumn, constants.columnHeader_Name());

            TextColumn<Flow> descriptionColumn = new TextColumn<Flow>() {
                @Override
                public String getValue(Flow content) {
                    return content.getContent().getDescription();
                }
            };
            table.addColumn(descriptionColumn, constants.columnHeader_Description());

            TextColumn<Flow> flowComponentsColumn = new TextColumn<Flow>() {
                @Override
                public String getValue(Flow content) {
                    return formatFlowComponents(content.getContent().getComponents());
                }
            };
            table.addColumn(flowComponentsColumn, constants.columnHeader_FlowComponents());

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
     * setFlows is called by the presenter, to push table data to the view
     * @param flows List of flows to view
     */
    @Override
    public void setFlows(List<Flow> flows) {
        table.setRowData(0, flows);
        table.setRowCount(flows.size());
        table.updateDone();
    }


    // Private methods

    /**
     * Formats a list of flow components to be shown in a cell
     * @param flowComponents
     * @return
     */
    private String formatFlowComponents(List<FlowComponent> flowComponents) {
        List<String> parameters = new ArrayList<String>();
        for(FlowComponent flowComponent : flowComponents){
            parameters.add(Format.inBracketsPairString(flowComponent.getContent().getName(), formatSvnRevision(flowComponent)));
        }
        return Format.commaSeparate(parameters);
    }

    private String formatSvnRevision(FlowComponent flowComponent){
        StringBuilder result = new StringBuilder();
        result.append("SVN Rev. ").append(flowComponent.getContent().getSvnRevision());
        return result.toString();
    }

}
