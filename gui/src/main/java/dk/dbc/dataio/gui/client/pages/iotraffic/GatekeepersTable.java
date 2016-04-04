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

package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.commons.types.GatekeeperDestination;

import java.util.List;

/**
 * Gatekeepers Table for the IoTraffic View
 */
public class GatekeepersTable extends CellTable {
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    ListDataProvider<GatekeeperDestination> dataProvider;

    /**
     * Constructor
     */
    public GatekeepersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructSubmitterColumn(), texts.label_Submitter());
        addColumn(constructPackagingColumn(), texts.label_Packaging());
        addColumn(constructFormatColumn(), texts.label_Format());
        addColumn(constructDestinationColumn(), texts.label_Destination());
        addColumn(constructCopyColumn(), texts.label_Copy());
    }

    /**
     * Puts data into the view
     *
     * @param gatekeepers The list of gatekeepers to put into the view
     */
    public void setGatekeepers(List<GatekeeperDestination> gatekeepers) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(gatekeepers);
    }


    /*
     * Local methods
     */

    private Column constructSubmitterColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getSubmitterNumber();
            }
        };
    }

    private Column constructPackagingColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getPackaging();
            }
        };
    }

    private Column constructFormatColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getFormat();
            }
        };
    }

    private Column constructDestinationColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.getDestination();
            }
        };
    }

    private Column constructCopyColumn() {
        return new TextColumn<GatekeeperDestination>() {
            @Override
            public String getValue(GatekeeperDestination gatekeeper) {
                return gatekeeper.isCopy() ? texts.label_DoCopy() : texts.label_DoNotCopy();
            }
        };
    }

}
