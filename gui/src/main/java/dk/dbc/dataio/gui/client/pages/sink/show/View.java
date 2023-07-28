package dk.dbc.dataio.gui.client.pages.sink.show;


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.model.SinkModel;

import java.util.List;

/**
 * This class is the View class for the Sinks Show View
 */
public class View extends ViewWidget {
    ListDataProvider<SinkModel> dataProvider;
    SingleSelectionModel<SinkModel> selectionModel = new SingleSelectionModel<>();

    /**
     * Default constructor
     */
    public View() {
        super("");
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


    /*
     * Private methods
     */


    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setSinks method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(sinksTable);

        sinksTable.addColumn(constructTypeColumn(), getTexts().columnHeader_Type());
        sinksTable.addColumn(constructNameColumn(), getTexts().columnHeader_Name());
        sinksTable.addColumn(constructDescriptionColumn(), getTexts().columnHeader_Description());
        sinksTable.addColumn(constructQueueNameColumn(), getTexts().columnHeader_QueueName());
        sinksTable.addColumn(constructActionColumn(), getTexts().columnHeader_Action());
        sinksTable.setSelectionModel(selectionModel);
        sinksTable.addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }

    /**
     * This method constructs the Type column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Type column
     */
    Column constructTypeColumn() {
        return new TextColumn<SinkModel>() {
            @Override
            public String getValue(SinkModel model) {
                return formatSinkType(model.getSinkType());
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
    Column constructDescriptionColumn() {
        return new TextColumn<SinkModel>() {
            @Override
            public String getValue(SinkModel model) {
                return model.getDescription();
            }
        };
    }

    Column constructQueueNameColumn() {
        return new TextColumn<SinkModel>() {
            @Override
            public String getValue(SinkModel model) {
                return model.getQueue();
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
                return getTexts().button_Edit();
            }
        };
        column.setFieldUpdater((index, model, buttonText) -> presenter.editSink((SinkModel) model));
        return column;
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    private DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> {
            SinkModel selected = selectionModel.getSelectedObject();
            if (selected != null) {
                presenter.editSink(selected);
            }
        };
    }

    private String formatSinkType(SinkContent.SinkType sinkType) {
        switch (sinkType) {
            case DPF:
                return getTexts().selection_DpfSink();
            case DUMMY:
                return getTexts().selection_DummySink();
            case ES:
                return getTexts().selection_ESSink();
            case OPENUPDATE:
                return getTexts().selection_UpdateSink();
            case IMS:
                return getTexts().selection_ImsSink();
            case HIVE:
                return getTexts().selection_HiveSink();
            case HOLDINGS_ITEMS:
                return getTexts().selection_HoldingsItemsSink();
            case TICKLE:
                return getTexts().selection_TickleSink();
            case WORLDCAT:
                return getTexts().selection_WorldCatSink();
            case MARCCONV:
                return getTexts().selection_MarcConvSink();
            case PERIODIC_JOBS:
                return getTexts().selection_PeriodicJobsSink();
            case VIP:
                return getTexts().selection_VipSink();
            case DMAT:
                return getTexts().selection_DMatSink();
            default:
                return "";
        }
    }

}
