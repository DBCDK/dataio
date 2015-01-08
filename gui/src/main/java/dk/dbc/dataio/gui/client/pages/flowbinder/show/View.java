package dk.dbc.dataio.gui.client.pages.flowbinder.show;


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the View class for the FlowBinders Show View
 */
public class View extends ViewWidget {
    ListDataProvider<FlowBinderModel> dataProvider;

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
     * @param flowBinderModels The list of flowbinders to put into the view
     */
    public void setFlowBinders(List<FlowBinderModel> flowBinderModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(flowBinderModels);
    }


    /**
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setFlowBinders method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<FlowBinderModel>();
        dataProvider.addDataDisplay(flowBindersTable);

        flowBindersTable.addColumn(constructNameColumn(), texts.columnHeader_Name());
        flowBindersTable.addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        flowBindersTable.addColumn(constructPackagingColumn(), texts.columnHeader_Packaging());
        flowBindersTable.addColumn(constructFormatColumn(), texts.columnHeader_Format());
        flowBindersTable.addColumn(constructCharsetColumn(), texts.columnHeader_Charset());
        flowBindersTable.addColumn(constructDestinationColumn(), texts.columnHeader_Destination());
        flowBindersTable.addColumn(constructRecordSplitterColumn(), texts.columnHeader_RecordSplitter());
        flowBindersTable.addColumn(constructSubmittersColumn(), texts.columnHeader_Submitters());
        flowBindersTable.addColumn(constructFlowColumn(), texts.columnHeader_Flow());
        flowBindersTable.addColumn(constructSinkColumn(), texts.columnHeader_Sink());
        flowBindersTable.addColumn(constructActionColumn(), texts.columnHeader_Action());
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
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
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getDescription();
            }
        };
    }

    /**
     * This method constructs the Packaging column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Packaging column
     */
    Column constructPackagingColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getPackaging();
            }
        };
    }

    /**
     * This method constructs the Format column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Format column
     */
    Column constructFormatColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getFormat();
            }
        };
    }

    /**
     * This method constructs the Charset column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Charset column
     */
    Column constructCharsetColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getCharset();
            }
        };
    }

    /**
     * This method constructs the Destination column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Destination column
     */
    Column constructDestinationColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getDestination();
            }
        };
    }

    /**
     * This method constructs the RecordSplitter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed RecordSplitter column
     */
    Column constructRecordSplitterColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getRecordSplitter();
            }
        };
    }

    /**
     * This method constructs the Submitters column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Submitters column
     */
    Column constructSubmittersColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                List<String> result = new ArrayList<String>();
                for (SubmitterModel submitterModel: model.getSubmitterModels()) {
                    result.add(Format.inBracketsPairString(submitterModel.getNumber(), submitterModel.getName()));
                }
                return Format.commaSeparate(result);
            }
        };
    }

    /**
     * This method constructs the Flow column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Flow column
     */
    Column constructFlowColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getFlowModel().getFlowName();
            }
        };
    }

    /**
     * This method constructs the Sink column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Sink column
     */
    Column constructSinkColumn() {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return model.getSinkModel().getSinkName();
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
        Column column = new Column<FlowBinderModel, String>(new ButtonCell()) {
            @Override
            public String getValue(FlowBinderModel model) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };
        column.setFieldUpdater(new FieldUpdater<FlowBinderModel, String>() {
            @Override
            public void update(int index, FlowBinderModel model, String buttonText) {
                presenter.editFlowBinder(model);
            }
        });
    return column;
    }

}
