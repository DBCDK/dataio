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

package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

import java.util.Comparator;
import java.util.List;


/**
 * Harvesters Table for the Harvester View
 */
public class HarvestersTable extends CellTable {
    private ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<TickleRepoHarvesterConfig> dataProvider;
    SingleSelectionModel<TickleRepoHarvesterConfig> selectionModel = new SingleSelectionModel<>();

    /**
     * Constructor
     */
    public HarvestersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructTickleIdColumn(), texts.columnHeader_TickleId());
        addColumn(constructDatasetNameColumn(), texts.columnHeader_Name());
        addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        addColumn(constructDestinationColumn(), texts.columnHeader_Destination());
        addColumn(constructFormatColumn(), texts.columnHeader_Format());
        addColumn(constructTypeColumn(), texts.columnHeader_Type());
        addColumn(constructStatusColumn(), texts.columnHeader_Status());
        addColumn(constructActionColumn(), texts.columnHeader_Action());

        setSelectionModel(selectionModel);
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }


    /**
     * This method sets the harvester data for the table
     * @param presenter The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<TickleRepoHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (TickleRepoHarvesterConfig TickleRepoHarvesterConfig: harvesters ) {
                dataProvider.getList().add(TickleRepoHarvesterConfig);
            }
        }
        (dataProvider.getList()).sort(Comparator.comparing(o -> o.getContent().getId()));
    }


    /*
     * Local methods
     * /

    /**
     * This method constructs the Id column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobId column
     */
    private Column constructTickleIdColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return harvester.getContent().getId();
            }
        };
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    private Column constructDatasetNameColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return harvester.getContent().getDatasetName();
            }
        };
    }

    /**
     * This method constructs the Description column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Description column
     */
    private Column constructDescriptionColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return harvester.getContent().getDescription();
            }
        };
    }

    /**
     * This method constructs the Destination column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Destination column
     */
    private Column constructDestinationColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return harvester.getContent().getDestination();
            }
        };
    }

    /**
     * This method constructs the Format column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Format column
     */
    private Column constructFormatColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return harvester.getContent().getFormat();
            }
        };
    }

    /**
     * This method constructs the Type column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Type column
     */
    private Column constructTypeColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return harvester.getContent().getType().toString();
            }
        };
    }

    /**
     * This method constructs the Status column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Status column
     */
    private Column constructStatusColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                        return harvester.getContent().isEnabled() ? texts.value_Enabled() : texts.value_Disabled();
            }
        };
    }

    /**
     * This method constructs the Action column
     * @return The constructed Action column
     */
    private Column constructActionColumn() {
        Column column = new Column<TickleRepoHarvesterConfig, String>(new ButtonCell()) {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };
        column.setFieldUpdater(new FieldUpdater<TickleRepoHarvesterConfig, String>() {
            @Override
            public void update(int index, TickleRepoHarvesterConfig config, String buttonText) {
                editTickleRepoHarvester(config);
            }
        });
        return column;
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler(){
        return doubleClickEvent -> editTickleRepoHarvester(selectionModel.getSelectedObject());
    }

    /**
     * Sends a request to the presenter for editing the harvester, passed as a parameter in the call
     * @param harvester The harvester to edit
     */
    private void editTickleRepoHarvester(TickleRepoHarvesterConfig harvester) {
        if (harvester != null) {
            presenter.editTickleRepoHarvesterConfig(String.valueOf(harvester.getId()));
        }
    }

}