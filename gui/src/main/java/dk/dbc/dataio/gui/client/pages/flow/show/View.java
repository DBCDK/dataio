package dk.dbc.dataio.gui.client.pages.flow.show;


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the View class for the Flows Show View
 */
public class View extends ViewWidget {

    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    ListDataProvider<FlowModel> dataProvider;
    SingleSelectionModel<FlowModel> selectionModel = new SingleSelectionModel<FlowModel>();

    public View() {
        super();
        setupColumns();
    }


    /**
     * This method is used to put data into the view
     *
     * @param flowModels The list of flows to put into the view
     */
    public void setFlows(List<FlowModel> flowModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(flowModels);
    }


    /**
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setFlows method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(flowsTable);

        flowsTable.addColumn(constructNameColumn(), getTexts().columnHeader_Name());
        flowsTable.addColumn(constructDescriptionColumn(), getTexts().columnHeader_Description());
        flowsTable.addColumn(constructFlowComponentsColumn(), getTexts().columnHeader_FlowComponents());
        flowsTable.addColumn(constructTimeOfFlowComponentUpdateColumn(), getTexts().columnHeader_TimeOfFlowComponentUpdate());
        flowsTable.addColumn(constructRefreshActionColumn(), getTexts().columnHeader_Action_Refresh());
        flowsTable.addColumn(constructEditActionColumn(), getTexts().columnHeader_Action_Edit());
        flowsTable.setSelectionModel(selectionModel);
        flowsTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
        return new TextColumn<FlowModel>() {
            @Override
            public String getValue(FlowModel model) {
                return model.getFlowName();
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
        return new TextColumn<FlowModel>() {
            @Override
            public String getValue(FlowModel model) {
                return model.getDescription();
            }
        };
    }

    /**
     * This method constructs the Flow Components column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Flow Components column
     */
    Column constructFlowComponentsColumn() {
        return new TextColumn<FlowModel>() {
            @Override
            public String getValue(FlowModel model) {
                return formatFlowComponents(model.getFlowComponents());
            }
        };
    }

    /**
     * This method constructs the timeOfFlowComponentUpdate column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed timeOfFlowComponentUpdate column
     */
    Column constructTimeOfFlowComponentUpdateColumn() {
        return new TextColumn<FlowModel>() {
            @Override
            public String getValue(FlowModel model) {
                return model.getTimeOfFlowComponentUpdate();
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
    Column constructRefreshActionColumn() {
        Column column = new Column<FlowModel, String>(new ButtonCell()) {
            @Override
            public String getValue(FlowModel model) {
                // The value to display in the button.
                return getTexts().button_Refresh();
            }
        };

        column.setFieldUpdater(new FieldUpdater<FlowModel, String>() {
            @Override
            public void update(int index, FlowModel model, String buttonText) {
                presenter.refreshFlowComponents(model);
            }
        });
        return column;
    }

    /**
     * This method constructs the Action column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Action column
     */
    @SuppressWarnings("unchecked")
    Column constructEditActionColumn() {
        Column column = new Column<FlowModel, String>(new ButtonCell()) {
            @Override
            public String getValue(FlowModel model) {
                // The value to display in the button.
                return getTexts().button_Edit();
            }
        };

        column.setFieldUpdater(new FieldUpdater<FlowModel, String>() {
            @Override
            public void update(int index, FlowModel model, String buttonText) {
                presenter.editFlow(model);
            }
        });
        return column;
    }


    // Private methods

    /**
     * Formats a list of flow components to be shown in a cell
     *
     * @param flowComponentModels A list of Flow Component Models
     * @return A list of text formatted Flow Components
     */
    private String formatFlowComponents(List<FlowComponentModel> flowComponentModels) {
        List<String> parameters = new ArrayList<String>();
        for (FlowComponentModel model : flowComponentModels) {
            parameters.add(Format.inBracketsPairString(model.getName(), formatRevision(model)));
        }
        return Format.commaSeparate(parameters);
    }

    /**
     * Formats a flow component to be shown in a cell.
     * If the svn next is empty, only the current svn revision will be displayed alongside
     * the flow component name. Otherwise both current and next revision will be shown alongside the flow component name.
     *
     * @param flowComponentModel The Flow Component Model
     * @return The text formatted Flow Component
     */
    private String formatRevision(FlowComponentModel flowComponentModel) {
        List<String> parameters = new ArrayList<String>();
        parameters.add(formatSvnRevision(flowComponentModel));
        if (flowComponentModel.getSvnNext() != null
                && !flowComponentModel.getSvnNext().isEmpty()) {
            parameters.add(formatSvnNext(flowComponentModel));
        }
        return Format.commaSeparate(parameters);
    }

    /**
     * Formats an SVN Revision number
     *
     * @param flowComponentModel The Flow Component Model
     * @return The formatted SVN Revision number
     */
    private String formatSvnRevision(FlowComponentModel flowComponentModel) {
        return "SVN Rev. " + flowComponentModel.getSvnRevision();
    }

    /**
     * Formats an SVN Next number
     *
     * @param flowComponentModel The Flow Component Model
     * @return The formatted SVN Next number
     */
    private String formatSvnNext(FlowComponentModel flowComponentModel) {
        return "SVN Next. " + flowComponentModel.getSvnNext();
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler() {
        DoubleClickHandler handler = new DoubleClickHandler() {
            @Override
            public void onDoubleClick(DoubleClickEvent doubleClickEvent) {
                FlowModel selected = selectionModel.getSelectedObject();
                if (selected != null) {
                    presenter.editFlow(selected);
                }
            }
        };
        return handler;
    }

    Texts getTexts() {
        return viewInjector.getTexts();
    }
}
