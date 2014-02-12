package dk.dbc.dataio.gui.client.pages.flowcomponentsshow;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import java.util.List;


/**
 *
 * This is the implementation of the Flow Components Show View
 *
 */
public class FlowComponentsShowViewImpl extends ContentPanel<FlowComponentsShowPresenter> implements FlowComponentsShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_FLOW_COMPONENTS_SHOW_WIDGET = "flowcomponentsshowwidget";

    // Local variables
    private final FlowComponentsShowConstants constants = GWT.create(FlowComponentsShowConstants.class);
    private DioCellTable<FlowComponent> table = new DioCellTable<FlowComponent>();


    /**
     * Constructor
     */
    public FlowComponentsShowViewImpl() {
        super(mainConstants.subMenu_FlowComponentsShow());
    }


    /**
     * Initializations of the view
     */
    public void init() {
        table.updateStarted();

        getElement().setId(GUIID_FLOW_COMPONENTS_SHOW_WIDGET);

        if (table.getColumnCount() == 0) {
            TextColumn<FlowComponent> nameColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    return content.getContent().getName();
                }
            };
            table.addColumn(nameColumn, constants.columnHeader_Name());

            TextColumn<FlowComponent> invocationMethodColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    return content.getContent().getInvocationMethod();
                }
            };
            table.addColumn(invocationMethodColumn, constants.columnHeader_InvocationMethod());

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
     * This method is called by the presenter, when pushing Flow Components to the view
     * @param flowComponents The flowcomponents to display
     */
    @Override
    public void setFlowComponents(List<FlowComponent> flowComponents) {
        table.setRowData(0, flowComponents);
        table.setRowCount(flowComponents.size());
        table.updateDone();
    }

}
