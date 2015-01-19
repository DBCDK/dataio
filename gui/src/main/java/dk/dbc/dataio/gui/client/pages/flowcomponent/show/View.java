package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;

import java.util.List;

public class View extends ViewWidget {
    ListDataProvider<FlowComponentModel> dataProvider;

    /**
     * Default constructor
     *
     * @param header The header text for the View
     * @param texts  The I8n texts for this view
     */
    public View(String header, dk.dbc.dataio.gui.client.pages.flowcomponent.show.Texts texts) {
        super(header, texts);
        setupColumns();
    }


    /**
     * This method is used to put data into the view
     *
     * @param flowComponentModels The list of flowcomponents to put into the view
     */
    public void setFlowComponents(List<FlowComponentModel> flowComponentModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(flowComponentModels);
    }


    /**
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setFlowComponents method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<FlowComponentModel>();
        dataProvider.addDataDisplay(flowComponentsTable);

        flowComponentsTable.addColumn(constructNameColumn(), texts.columnHeader_Name());
        flowComponentsTable.addColumn(constructJavaScriptNameColumn(), texts.columnHeader_ScriptName());
        flowComponentsTable.addColumn(constructInvocationMethodColumn(), texts.columnHeader_InvocationMethod());
        flowComponentsTable.addColumn(constructSvnProjectColumn(), texts.columnHeader_Project());
        flowComponentsTable.addColumn(constructSvnRevisionColumn(), texts.columnHeader_Revision());
        flowComponentsTable.addColumn(constructJavaScriptModulesColumn(), texts.columnHeader_JavaScriptModules());
        flowComponentsTable.addColumn(constructActionColumn(), texts.columnHeader_Action());
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getName();
            }
        };
    }

    /**
     * This method constructs the Script Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Script Name column
     */
    Column constructJavaScriptNameColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getInvocationJavascript();
            }
        };
    }

    /**
     * This method constructs the Invocation Method column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Invocation Method column
     */
    Column constructInvocationMethodColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getInvocationMethod();
            }
        };
    }

    /**
     * This method constructs the SVN Project column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SVN Project column
     */
    Column constructSvnProjectColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getSvnProject();
            }
        };
    }

    /**
     * This method constructs the SVN Revision column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SVN Revision column
     */
    Column constructSvnRevisionColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getSvnRevision();
            }
        };
    }

    /**
     * This method constructs the Java Script Modules column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Java Script Modules column
     */
    Column constructJavaScriptModulesColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return fomatJavaScriptModules(model.getJavascriptModules());
            }
        };
    }

    /**
     * This method constructs the Action column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Action column
     */
    @SuppressWarnings("unchecked")
    Column constructActionColumn() {
        Column column = new Column<FlowComponentModel, String>(new ButtonCell()) {
            @Override
            public String getValue(FlowComponentModel model) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };

        column.setFieldUpdater(new FieldUpdater<FlowComponentModel, String>() {
            @Override
            public void update(int index, FlowComponentModel model, String buttonText) {
                presenter.editFlowComponent(model);
            }
        });
        return column;
    }

    /**
     * This method format a list of Java Script Module names to a String
     * @param javascriptModules The list of Java Script Names
     * @return A string containing a comma-separated list of Java Script Name
     */
    private String fomatJavaScriptModules(List<String> javascriptModules) {
        String moduleNames = "";
        for (String script: javascriptModules) {
            if (moduleNames.isEmpty()) {
                moduleNames = script;
            } else {
                moduleNames += ", " + script;
            }
        }
        return moduleNames;
    }

}
