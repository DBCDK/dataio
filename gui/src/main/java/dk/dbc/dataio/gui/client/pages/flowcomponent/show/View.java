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

package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;

import java.util.List;

public class View extends ViewWidget {
    ListDataProvider<FlowComponentModel> dataProvider;
    SingleSelectionModel<FlowComponentModel> selectionModel = new SingleSelectionModel<FlowComponentModel>();

    public View() {
        super("");
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

        Texts texts = getTexts();
        flowComponentsTable.addColumn(constructNameColumn(), texts.columnHeader_Name());
        flowComponentsTable.addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        flowComponentsTable.addColumn(constructJavaScriptNameColumn(), texts.columnHeader_ScriptName());
        flowComponentsTable.addColumn(constructInvocationMethodColumn(), texts.columnHeader_InvocationMethod());
        flowComponentsTable.addColumn(constructSvnProjectColumn(), texts.columnHeader_Project());
        flowComponentsTable.addColumn(constructSvnRevisionColumn(), texts.columnHeader_Revision());
        flowComponentsTable.addColumn(constructSvnNextColumn(), texts.columnHeader_Next());
        flowComponentsTable.addColumn(constructJavaScriptModulesColumn(), texts.columnHeader_JavaScriptModules());
        flowComponentsTable.addColumn(constructActionColumn(), texts.columnHeader_Action());
        flowComponentsTable.setSelectionModel(selectionModel);
        flowComponentsTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
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
     * This method constructs the Description column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Description column
     */
    Column constructDescriptionColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getDescription();
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
     * This method constructs the Next SVN Revision column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Next SVN Revision column
     */
    Column constructSvnNextColumn() {
        return new TextColumn<FlowComponentModel>() {
            @Override
            public String getValue(FlowComponentModel model) {
                return model.getSvnNext();
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
                return getTexts().button_Edit();
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

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler(){
        DoubleClickHandler handler = new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent doubleClickEvent) {
                FlowComponentModel selected = selectionModel.getSelectedObject();
                if(selected != null) {
                    presenter.editFlowComponent(selected);
                }
            }
        };
        return handler;
    }

}
