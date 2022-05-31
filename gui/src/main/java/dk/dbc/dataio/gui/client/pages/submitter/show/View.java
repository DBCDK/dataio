package dk.dbc.dataio.gui.client.pages.submitter.show;


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

import java.util.Comparator;
import java.util.List;

/**
 * This class is the View class for the Submitters Show View
 */
public class View extends ViewWidget {
    ListDataProvider<SubmitterModel> dataProvider;
    SingleSelectionModel<SubmitterModel> selectionModel = new SingleSelectionModel<>();
    private final Texts texts = getTexts();

    public View() {
        super("");
        setupColumns();
    }

    /**
     * This method is used to put data into the view
     *
     * @param submitterModels The list of submitters to put into the view
     */
    public void setSubmitters(List<SubmitterModel> submitterModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(submitterModels);
    }

    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setSubmitters method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(submittersTable);

        submittersTable.addColumn(constructSubmitterNumberColumn(), texts.columnHeader_Number());
        submittersTable.addColumn(constructNameColumn(), texts.columnHeader_Name());
        submittersTable.addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        submittersTable.addColumn(constructStatusColumn(), texts.columnHeader_Status());
        submittersTable.addColumn(constructFlowBindersColumn(), texts.columnHeader_FlowBinders());
        submittersTable.addColumn(constructActionColumn(), texts.columnHeader_Action());
        submittersTable.setSelectionModel(selectionModel);
        submittersTable.addDomHandler(doubleClickEvent -> presenter.editSubmitter(selectionModel.getSelectedObject()), DoubleClickEvent.getType());
    }

    /**
     * Shows the flowbinders passed as a parameter in the call to the method
     * as elements in the popup list in alphabetically order
     *
     * @param flowBinders The flowbinders to display
     */
    void showFlowBinders(List<FlowBinderModel> flowBinders) {
        if (flowBinders != null) {
            flowBinders.sort(Comparator.comparing(FlowBinderModel::getName));
            popupList.clear();
            for (FlowBinderModel model : flowBinders) {
                popupList.addItem(model.getName(), Long.toString(model.getId()));
            }
            popupList.show();
        }
    }

    /**
     * This method constructs the SubmitterNumber column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed SubmitterNumber column
     */
    Column constructSubmitterNumberColumn() {
        return new TextColumn<SubmitterModel>() {
            @Override
            public String getValue(SubmitterModel model) {
                return model.getNumber();
            }
        };
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
        return new TextColumn<SubmitterModel>() {
            @Override
            public String getValue(SubmitterModel model) {
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
        return new TextColumn<SubmitterModel>() {
            @Override
            public String getValue(SubmitterModel model) {
                return model.getDescription();
            }
        };
    }

    /**
     * This method constructs the Status column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Status column
     */
    Column constructStatusColumn() {
        return new TextColumn<SubmitterModel>() {
            @Override
            public String getValue(SubmitterModel model) {
                return model.isEnabled() ? "" : texts.value_Disabled();
            }
        };
    }

    /**
     * This method constructs the Flowbinders column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Flowbinders column
     */
    @SuppressWarnings("unchecked")
    Column constructFlowBindersColumn() {
        Column column = new Column<SubmitterModel, String>(new ButtonCell()) {
            @Override
            public String getValue(SubmitterModel model) {
                // The value to display in the button.
                return texts.button_ShowFlowBinders();
            }
        };

        column.setFieldUpdater((FieldUpdater<SubmitterModel, String>) (index, model, buttonText) -> presenter.showFlowBinders(model));
        return column;
    }

    /**
     * This method constructs the Action column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Action column
     */
    @SuppressWarnings("unchecked")
    Column constructActionColumn() {
        Column column = new Column<SubmitterModel, String>(new ButtonCell()) {
            @Override
            public String getValue(SubmitterModel model) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };

        column.setFieldUpdater((FieldUpdater<SubmitterModel, String>) (index, model, buttonText) -> presenter.editSubmitter(model));
        return column;
    }

}
