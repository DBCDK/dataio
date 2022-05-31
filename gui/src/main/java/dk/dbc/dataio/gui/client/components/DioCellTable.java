package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.cellview.client.CellTable;

/**
 * Specialization of the CellTable class.
 * Maintains an "updated done" style name upon call of the updateStarted() and updateDone() methods.
 * Selenium can then easily determine, when an update has been done.
 *
 * @param <T> the type of the object
 */
public class DioCellTable<T> extends CellTable<T> {
    public final static String DIO_CELLTABLE_UPDATE_DONE = "dio-celltable-update-done";

    /**
     * This method should be called upon start of an update, in order to signal to
     * Selenium, that data is being loaded into the table.
     */
    public void updateStarted() {
        removeStyleName(DIO_CELLTABLE_UPDATE_DONE);
    }

    /**
     * This method should be called upon completion of an update, in order to signal to
     * Selenium, that data is ready.
     */
    public void updateDone() {
        addStyleName(DIO_CELLTABLE_UPDATE_DONE);
    }
}
