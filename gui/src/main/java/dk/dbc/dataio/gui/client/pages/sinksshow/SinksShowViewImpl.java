package dk.dbc.dataio.gui.client.pages.sinksshow;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import java.util.List;

/**
 * Show Sinks view implementation
 * Shows a table, containing:
 *  o Sink Name
 *  o Sink Resource Name
 *  o Edit button
 */
public class SinksShowViewImpl extends ContentPanel<SinksShowPresenter> implements SinksShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_SINKS_SHOW_WIDGET = "sinksshowwidget";

    // Local variables
    private final static SinksShowConstants constants = GWT.create(SinksShowConstants.class);
    private final DioCellTable<Sink> table = new DioCellTable<Sink>();

    /**
     * Constructor
     */
    public SinksShowViewImpl() {
        super(constants.menu_Sinks());
    }

    /**
     * Initializations of the view
     * Sets up the three columns in the CellTable
     * First column contains the name of the sink
     * Second column contains the name of the sink resource
     * Third column contains the ButtonCell used to edit the individual sinks
     */
    public void init() {
        table.updateStarted();

        getElement().setId(GUIID_SINKS_SHOW_WIDGET);

        if (table.getColumnCount() == 0) {
            TextColumn<Sink> sinkNameColumn = new TextColumn<Sink>() {
                @Override
                public String getValue(Sink sink) {
                    return sink.getContent().getName();
                }
            };
            table.addColumn(sinkNameColumn, constants.columnHeader_Name());


            TextColumn<Sink> resourceName = new TextColumn<Sink>() {
                @Override
                public String getValue(Sink sink) {
                    return sink.getContent().getResource();
                }
            };
            table.addColumn(resourceName, constants.columnHeader_ResourceName());


            Column editButtonColumn = new Column<Sink, String>(new ButtonCell()) {
                @Override
                public String getValue(Sink object) {
                    // The value to display in the button.
                    return constants.button_Edit();
                }
            };

            // Handler: Registering key clicks (on the buttonCell available for each sink).
            // Clicks on ButtonCells are handled by setting the FieldUpdater for the Column
            editButtonColumn.setFieldUpdater(new FieldUpdater<Sink, String>() {
                @Override
                public void update(int index, Sink sink, String buttonText) {
                    editClick(sink);
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
     * setSinks is called by the presenter, to push table data to the view
     * @param sinks List of sinks to view
     */
    @Override
    public void setSinks(List<Sink> sinks) {
        table.setPageSize(sinks.size());
        table.setRowData(0, sinks);
        table.setRowCount(sinks.size());
        table.updateDone();
    }

    // Private methods

    /**
     * When a key click has been registered, the editSink method in SinkShowPresenter is called in order to handle the edit sink action itself.
     * @param sink The sink to edit
     */
    private void editClick(Sink sink){
        presenter.editSink(sink);
    }

    /*
    * Private classes
    */
}
