package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.i18n.SubmittersShowConstants;
import dk.dbc.dataio.gui.client.presenters.SubmittersShowPresenter;
import java.util.List;

/**
 * Show Submitters view implementation
 * Shows a table, containing:
 *  o Name
 *  o Description
 */
public class SubmittersShowViewImpl extends ContentPanel<SubmittersShowPresenter> implements SubmittersShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_SUBMITTERS_SHOW_WIDGET = "submittersshowwidget";

    // Local variables
    private final SubmittersShowConstants constants = GWT.create(SubmittersShowConstants.class);
    private final CellTable<Submitter> table = new CellTable<Submitter>();


    /**
     * Constructor
     */
    public SubmittersShowViewImpl() {
        super(mainConstants.mainMenu_Submitters());
    }

    /**
     * Initializations of the view
     * Sets up the three columns in the CellTable
     */
    public void init() {
        getElement().setId(GUIID_SUBMITTERS_SHOW_WIDGET);
        if (table.getColumnCount() == 0) {
            TextColumn<Submitter> numberColumn = new TextColumn<Submitter>() {
                @Override
                public String getValue(Submitter content) {
                    return Long.toString(content.getContent().getNumber());
                }
            };
            table.addColumn(numberColumn, constants.columnHeader_Number());

            TextColumn<Submitter> nameColumn = new TextColumn<Submitter>() {
                @Override
                public String getValue(Submitter content) {
                    return content.getContent().getName();
                }
            };
            table.addColumn(nameColumn, constants.columnHeader_Name());

            TextColumn<Submitter> descriptionColumn = new TextColumn<Submitter>() {
                @Override
                public String getValue(Submitter content) {
                    return content.getContent().getDescription();
                }
            };
            table.addColumn(descriptionColumn, constants.columnHeader_Description());

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
     * setSubmitters is called by the presenter, to push table data to the view
     * @param submitters List of submitters to view
     */
    @Override
    public void setSubmitters(List<Submitter> submitters) {
        table.setRowData(0, submitters);
        table.setRowCount(submitters.size());
    }

}
