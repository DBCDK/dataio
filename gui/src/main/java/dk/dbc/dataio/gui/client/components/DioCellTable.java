
package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.cellview.client.CellTable;

/**
 * Specialization of the CellTable class.
 * Adds an "updated done" stylename upon call of the updateDone() method.
 * Selenium can then easily determine, when an update has been done.
 *
 * @param <T>
 */
public class DioCellTable<T> extends CellTable<T> {
    public final static String DIO_CELLTABLE_UPDATE_DONE = "dio-celltable-update-done";

    /**
     * This method should be called upon compeletion of an update, in order to signal to
     * Selenium, that data is ready.
     */
    public void updateDone() {
        addStyleName(DIO_CELLTABLE_UPDATE_DONE);
    }
}
