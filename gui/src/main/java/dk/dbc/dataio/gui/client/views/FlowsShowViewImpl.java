package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.i18n.FlowsShowConstants;
import dk.dbc.dataio.gui.client.presenters.FlowsShowPresenter;
import java.util.List;

/**
 * Show Flows view implementation
 * Shows a table, containing:
 *  o Name
 *  o Description
 *  o List of Flowcomponents, which are the Flowcomponent names separated by commas
 */
public class FlowsShowViewImpl extends FlowPanel implements FlowsShowView {

    // public Identifiers
    public static final String GUIID_FLOWS_SHOW_WIDGET = "flowsshowwidget";

    // private objects
//    private FlowsShowPresenter presenter;
    private final FlowsShowConstants constants = GWT.create(FlowsShowConstants.class);

    private CellTable<Flow> table = new CellTable<Flow>();

    /**
     * Constructor
     * Sets up the three columns in the CellTable
     */
    public FlowsShowViewImpl() {
        super();
        getElement().setId(GUIID_FLOWS_SHOW_WIDGET);

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

    /**
     * setPresenter
     * @param presenter
     */
    @Override
    public void setPresenter(FlowsShowPresenter presenter) {
//        this.presenter = presenter;
    }

    /**
     * onFailure is called by the presenter upon failure
     * @param message
     */
    @Override
    public void onFailure(String message) {
        Window.alert("Error: " + message);
    }

    /**
     * onFailure is called by the presenter upon successful operation
     * @param message
     */
    @Override
    public void onSuccess(String message) {
    }

    /**
     * refresh
     */
    @Override
    public void refresh() {
    }

    /**
     * setFlows is called by the presenter, to push table data to the view
     * @param flows List of flows to view
     */
    @Override
    public void setFlows(List<Flow> flows) {
        table.setRowData(0, flows);
        table.setRowCount(flows.size());
    }


    // Private methods

    /**
     * Formats a list of flow components to be shown in a cell
     * @param flowComponents
     * @return
     */
    private String formatFlowComponents(List<FlowComponent> flowComponents) {
        String result = "";
        for (FlowComponent component: flowComponents) {
            if (!result.isEmpty()) {
                result += ", ";
            }
            result += component.getContent().getName();
        }
        return result;
    }

}
