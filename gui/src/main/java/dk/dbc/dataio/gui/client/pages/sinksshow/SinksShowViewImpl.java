package dk.dbc.dataio.gui.client.pages.sinksshow;

import com.google.gwt.core.client.GWT;
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
        table.setRowData(0, sinks);
        table.setRowCount(sinks.size());
        table.updateDone();
    }


    // Private methods

}
