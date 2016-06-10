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

package dk.dbc.dataio.gui.client.pages.harvester.ush.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;

import java.util.List;

public class View extends ContentPanel<Presenter> implements IsWidget {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, View> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField(provided=true) HarvestersTable harvestersTable;


    /**
     * Default empty constructor
     */
    public View() {
        this("");
    }

    /**
     * Default constructor
     * @param header Header text
     */
    public View(String header) {
        super(header);
        harvestersTable = new HarvestersTable();
        add(uiBinder.createAndBindUi(this));
    }

    protected Texts getTexts() {
        return this.viewInjector.getTexts();
    }

    public void setHarvesters(List<UshSolrHarvesterConfig> harvesters) {
        harvestersTable.setHarvesters(presenter, harvesters);
    }

}
