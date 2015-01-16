package dk.dbc.dataio.gui.client.pages.sink.show;


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.model.SinkModel;

import java.util.List;

/**
 * This class is the View class for the Sinks Show View
 */
public class View extends ViewWidget {
    ListDataProvider<SinkModel> dataProvider;

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
     * @param sinkModels The list of sinks to put into the view
     */
    public void setSinks(List<SinkModel> sinkModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(sinkModels);
    }


    /**
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setSinks method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<SinkModel>();
        dataProvider.addDataDisplay(sinksTable);

        sinksTable.addColumn(constructNameColumn(), texts.columnHeader_Name());
        sinksTable.addColumn(constructResourceNameColumn(), texts.columnHeader_ResourceName());
        sinksTable.addColumn(constructActionColumn(), texts.columnHeader_Action());
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
        return new TextColumn<SinkModel>() {
            @Override
            public String getValue(SinkModel model) {
                return model.getSinkName();
            }
        };
    }

    /**
     * This method constructs the Resource Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Resource Name column
     */
    Column constructResourceNameColumn() {
        return new TextColumn<SinkModel>() {
            @Override
            public String getValue(SinkModel model) {
                return model.getResourceName();
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
        Column column = new Column<SinkModel, String>(new ButtonCell()) {
            @Override
            public String getValue(SinkModel model) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };

        column.setFieldUpdater(new FieldUpdater<SinkModel, String>() {
            @Override
            public void update(int index, SinkModel model, String buttonText) {
                presenter.editSink(model);
            }
        });
    return column;
    }

}
