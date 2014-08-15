package dk.dbc.dataio.gui.client.pages.flowcomponentsshow;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.JavaScript;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;


/**
 *
 * This is the implementation of the Flow Components Show View
 *
 */
public class FlowComponentsShowGenericViewImpl extends ContentPanel<FlowComponentsShowGenericPresenter> implements FlowComponentsShowGenericView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_FLOW_COMPONENTS_SHOW_WIDGET = "flowcomponentsshowwidget";
    public static final String CLASS_FLOW_COMPONENTS_SHOW_WIDGET_EDIT_BUTTON = "flowcomponentsshowwidget_editbutton";

    // Local variables
    private final static FlowComponentsShowConstants constants = GWT.create(FlowComponentsShowConstants.class);
    private final DioCellTable<FlowComponent> table = new DioCellTable<FlowComponent>();


    /**
     * Constructor
     */
    public FlowComponentsShowGenericViewImpl() {
        super(constants.menu_FlowComponentsShow());
    }


    /**
     * Initializations of the view
     */
    public void init() {
        table.updateStarted();

        getElement().setId(GUIID_FLOW_COMPONENTS_SHOW_WIDGET);

        if (table.getColumnCount() == 0) {
            // Navn
            TextColumn<FlowComponent> nameColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    return content.getContent().getName();
                }
            };
            table.addColumn(nameColumn, constants.columnHeader_Name());

            // Script navn
            TextColumn<FlowComponent> scriptNameColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    return content.getContent().getInvocationJavascriptName();
                }
            };
            table.addColumn(scriptNameColumn, constants.columnHeader_ScriptName());

            // Script start metode
            TextColumn<FlowComponent> invocationMethodColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    return content.getContent().getInvocationMethod();
                }
            };
            table.addColumn(invocationMethodColumn, constants.columnHeader_InvocationMethod());

            // SVN Projekt
            TextColumn<FlowComponent> svnProjectColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    return content.getContent().getSvnProjectForInvocationJavascript();
                }
            };
            table.addColumn(svnProjectColumn, constants.columnHeader_Project());

            // SVN Revision
            TextColumn<FlowComponent> svnRevisionColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    return Long.toString(content.getContent().getSvnRevision());
                }
            };
            table.addColumn(svnRevisionColumn, constants.columnHeader_Revision());

            // Javascriptmoduler
            TextColumn<FlowComponent> javaScriptModulesColumn = new TextColumn<FlowComponent>() {
                @Override
                public String getValue(FlowComponent content) {
                    String moduleNames = "";
                    for (JavaScript script: content.getContent().getJavascripts()) {
                        if (moduleNames.isEmpty()) {
                            moduleNames = script.getModuleName();
                        } else {
                            moduleNames = moduleNames.concat(", ").concat(script.getModuleName());
                        }
                    }
                    return moduleNames;
                }
            };
            table.addColumn(javaScriptModulesColumn, constants.columnHeader_JavaScriptModules());

            Column editButtonColumn = new Column<FlowComponent, String>(new ButtonCell()) {
                @Override
                public String getValue(FlowComponent flowComponent) {
                    // The value to display in the button.
                    return constants.button_Edit();
                }
            };

            //Define class name for the button element
            editButtonColumn.setCellStyleNames(CLASS_FLOW_COMPONENTS_SHOW_WIDGET_EDIT_BUTTON);

            // Handler: Registering key clicks (on the buttonCell available for each flow component).
            // Clicks on ButtonCells are handled by setting the FieldUpdater for the Column
            editButtonColumn.setFieldUpdater(new FieldUpdater<FlowComponent, String>() {
                @Override
                public void update(int index, FlowComponent flowComponent, String buttonText) {
                    editClick(flowComponent);
                }
            });

            table.addColumn(editButtonColumn, constants.columnHeader_Action());
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
     * This method is called by the presenter, when pushing Flow Components to the view
     * @param flowComponents The flowcomponents to display
     */
    @Override
    public void setFlowComponents(List<FlowComponent> flowComponents) {
        table.setPageSize(flowComponents.size());
        table.setRowData(0, flowComponents);
        table.setRowCount(flowComponents.size());
        table.updateDone();
    }

    /**
     * When a key click has been registered, the editFlowComponent method in FlowComponentsShowPresenter is called in order to handle the edit sink action itself.
     * @param flowComponent The flow component to edit
     */
    private void editClick(FlowComponent flowComponent){
        presenter.editFlowComponent(flowComponent);
    }

}
