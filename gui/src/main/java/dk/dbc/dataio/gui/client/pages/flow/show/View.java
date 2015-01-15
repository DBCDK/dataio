package dk.dbc.dataio.gui.client.pages.flow.show;


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the View class for the Flows Show View
 */
public class View extends ViewWidget {
    ListDataProvider<FlowModel> dataProvider;

    /**
     * Default constructor
     *
     * @param header The header text for the View
     * @param texts  The I8n texts for this view
     */
    public View(String header, Texts texts) {
        super(header, texts);
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
        dataProvider = new ListDataProvider<FlowModel>();
        dataProvider.addDataDisplay(flowsTable);

        flowsTable.addColumn(constructNameColumn(), texts.columnHeader_Name());
        flowsTable.addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        flowsTable.addColumn(constructFlowComponentsColumn(), texts.columnHeader_FlowComponents());
        flowsTable.addColumn(constructRefreshActionColumn(), texts.columnHeader_Action());
        flowsTable.addColumn(constructEditActionColumn(), texts.columnHeader_Action());
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
                return texts.button_Refresh();
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
                return texts.button_Update();
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
     * @param flowComponentModels A list of Flow Component Models
     * @return A list of text formatted Flow Components
     */
    private String formatFlowComponents(List<FlowComponentModel> flowComponentModels) {
        List<String> parameters = new ArrayList<String>();
        for(FlowComponentModel model : flowComponentModels){
            parameters.add(Format.inBracketsPairString(model.getName(), formatSvnRevision(model)));
        }
        return Format.commaSeparate(parameters);
    }

    /**
     * Formats an SVN Revision number
     * @param flowComponentModel The Flow Component Model
     * @return The formatted SVN Revision number
     */
    private String formatSvnRevision(FlowComponentModel flowComponentModel){
        StringBuilder result = new StringBuilder();
        result.append("SVN Rev. ").append(flowComponentModel.getSvnRevision());
        return result.toString();
    }

}
