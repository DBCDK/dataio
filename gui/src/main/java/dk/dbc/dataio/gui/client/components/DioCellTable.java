/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */


package dk.dbc.dataio.gui.client.components;

import com.google.gwt.user.cellview.client.CellTable;

/**
 * Specialization of the CellTable class.
 * Maintains an "updated done" style name upon call of the updateStarted() and updateDone() methods.
 Selenium can then easily determine, when an update has been done.
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
