package dk.dbc.dataio.gui.client.pages.harvester.harvestersShow;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;

/**
 * Created by sma on 25/04/14.
 */
public class HarvestersShowViewImpl extends ContentPanel<HarvestersShowPresenter> implements HarvestersShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_HARVESTERS_SHOW_WIDGET = "harvestersshowwidget";
    public static final String GUUID_SHOW_HARVESTERS_TABLE = "showharvesterstable_id";
    public static final String CLASS_HARVESTER_SHOW_WIDGET_START_STOP_BUTTON = "harvestersshowwidget_startstopbutton";

    // Local variables
    private final static HarvestersShowConstants constants = GWT.create(HarvestersShowConstants.class);
    private final DioCellTable<String> table = new DioCellTable<String>();

    /**
     * Constructor
     */
    public HarvestersShowViewImpl() {
        super(constants.menu_Harvesters());
    }

    /**
     * Initializations of the view
     * Sets up the three columns in the CellTable
     * First column contains the name of the harvester
     * Second column contains the status of the harvester (running/stopped)
     * Third column contains the ButtonCell used to start or stop individual harvesters.
     */
    public void init() {
        table.updateStarted();
        table.getElement().setId(GUUID_SHOW_HARVESTERS_TABLE);

        getElement().setId(GUIID_HARVESTERS_SHOW_WIDGET);

        if (table.getColumnCount() == 0) {
            TextColumn<String> harvesterNameColumn = new TextColumn<String>() {
                @Override
                public String getValue(String name) {
                    // The name of the harvester.
                    return name;
                }
            };
            table.addColumn(harvesterNameColumn, constants.columnHeader_Name());


            TextColumn<String> harvesterStatusColumn = new TextColumn<String>() {
                @Override
                public String getValue(String status) {
                    // The status of the harvester (running /stopped)
                    return "Running";
                }
            };
            table.addColumn(harvesterStatusColumn, constants.columnHeader_Status());


            Column startStopButtonColumn = new Column<String, String>(new ButtonCell()) {
                @Override
                public String getValue(String status) {

                    // The value to display in the button.
                    return constants.button_Stop();
                }
            };

            //Define class name for the button element
            startStopButtonColumn.setCellStyleNames(CLASS_HARVESTER_SHOW_WIDGET_START_STOP_BUTTON);

            //TODO
            // Handler: Registering key clicks (on the buttonCell available for each harvester).
            // Clicks on ButtonCells are handled by setting the FieldUpdater for the Column


            table.addColumn(startStopButtonColumn, constants.columnHeader_Action());

            add(table);
        }
    }


    /*
     * Implementation of interface methods
     */

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
    public void setStatusText(String message) {
    }

    /**
     * OnFailure
     * @param message The message to display to the user
     */
    @Override
    public void setErrorText(String message) {

    }

    @Override
    public void setHarvesters(List<String> harvesters) {
        table.setPageSize(harvesters.size());
        table.setRowData(0, harvesters);
        table.setRowCount(harvesters.size());
        table.updateDone();
    }
}