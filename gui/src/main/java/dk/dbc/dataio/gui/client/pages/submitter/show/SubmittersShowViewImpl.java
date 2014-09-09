package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.components.DioCellTable;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;

/**
 * Show Submitters view implementation
 * Shows a table, containing:
 *  o Number
 *  o Name
 *  o Description
 */
public class SubmittersShowViewImpl extends ContentPanel<SubmittersShowPresenter> implements SubmittersShowView {
    // Constants (These are not all private since we use them in the selenium tests)
    public static final String GUIID_SUBMITTERS_SHOW_WIDGET = "submittersshowwidget";
    public static final String CLASS_SUBMITTERS_SHOW_WIDGET_EDIT_BUTTON = "submittersshowwidget_editbutton";

    // Local variables
    private final static SubmittersShowTexts constants = GWT.create(SubmittersShowTexts.class);
    private final DioCellTable<Submitter> table = new DioCellTable<Submitter>();


    /**
     * Constructor
     */
    public SubmittersShowViewImpl() {
        super(constants.menu_Submitters());
    }

    /**
     * Initializations of the view
     * Sets up the three columns in the CellTable
     */
    public void init() {
        table.updateStarted();

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

            Column editButtonColumn = new Column<Submitter, String>(new ButtonCell()) {
                @Override
                public String getValue(Submitter submitter) {
                    // The value to display in the button.
                    return constants.button_Edit();
                }
            };

            //Define class name for the button element
            editButtonColumn.setCellStyleNames(CLASS_SUBMITTERS_SHOW_WIDGET_EDIT_BUTTON);

            // Handler: Registering key clicks (on the buttonCell available for each submitter).
            editButtonColumn.setFieldUpdater(new FieldUpdater<Submitter, String>() {
                @Override
                public void update(int index, Submitter submitter, String buttonText) {
                    editClick(submitter);
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
        super.setErrorText(message);
        table.updateDone();
    }

    /**
     * setSubmitters is called by the presenter, to push table data to the view
     * @param submitters List of submitters to view
     */
    @Override
    public void setSubmitters(List<Submitter> submitters) {
        table.setPageSize(submitters.size());
        table.setRowData(0, submitters);
        table.setRowCount(submitters.size());
        table.updateDone();
    }

    /**
     * When a key click has been registered, the editSubmitter method in SubmitterShowPresenter is called,
     * in order to handle the edit submitter action itself.
     * @param submitter The submitter to edit
     */
    private void editClick(Submitter submitter){
        presenter.editSubmitter(submitter);
    }

}
