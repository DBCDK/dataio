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

package dk.dbc.dataio.gui.client.pages.flowcomponent.show.jsmodulespopup;

import com.google.gwt.uibinder.client.UiConstructor;
import dk.dbc.dataio.gui.client.components.PopupBox;

import java.util.List;


/**
 * Popup box for showing a double list in a popup window
 */
public class PopupDoubleList extends PopupBox<DoubleList, PopupDoubleList.DoubleListData> {


    /**
     * Constructor
     *
     * @param dialogTitle  The title text to display on the Dialog Box (mandatory)
     * @param okButtonText The text to be displayed in the OK Button (mandatory)
     */
    @UiConstructor
    public PopupDoubleList(String dialogTitle, String okButtonText) {
        super(new DoubleList(), dialogTitle, okButtonText);
    }

    public PopupDoubleList() {
        this("", "");
    }


    /*
     * Local classes
     */

    public class DoubleListData {
        String headerLeft;
        List<String> bodyLeft;
        String headerRight;
        List<String> bodyRight;

        public DoubleListData(String headerLeft, List<String> bodyLeft, String headerRight, List<String> bodyRight) {
            this.headerLeft = headerLeft;
            this.bodyLeft = bodyLeft;
            this.headerRight = headerRight;
            this.bodyRight = bodyRight;
        }
    }

}
