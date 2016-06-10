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

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;

import java.util.Collections;
import java.util.List;


/**
 * Harvesters Table for the Harvester View
 */
public class HarvestersTable extends CellTable {
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<UshSolrHarvesterConfig> dataProvider;
    SingleSelectionModel<UshSolrHarvesterConfig> selectionModel = new SingleSelectionModel<>();

    /**
     * Constructor
     */
    public HarvestersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructJobIdColumn(), textWithToolTip(texts.columnHeader_JobId(), texts.help_JobId()));
        addColumn(constructNameColumn(), textWithToolTip(texts.columnHeader_Name(), texts.help_Name()));
        addColumn(constructStatusColumn(), textWithToolTip(texts.columnHeader_Status(), texts.help_Status()));
        addColumn(constructLatestHarvestColumn(), textWithToolTip(texts.columnHeader_LatestHarvest(), texts.help_LatestHarvest()));
        addColumn(constructCountColumn(), textWithToolTip(texts.columnHeader_Count(), texts.help_Count()));
        addColumn(constructNextHarverstColumn(), textWithToolTip(texts.columnHeader_NextHarverst(), texts.help_NextHarverst()));
        addColumn(constructStatusMessageColumn(), textWithToolTip(texts.columnHeader_StatusMessage(), texts.help_StatusMessage()));
        addColumn(constructEnabledColumn(), textWithToolTip(texts.columnHeader_Enabled(), texts.help_Enabled()));
        addColumn(constructActionColumn(), textWithToolTip(texts.columnHeader_Action(), texts.help_Action()));

        setSelectionModel(selectionModel);
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }


    /**
     * This method sets the harvester data for the table
     * @param presenter The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<UshSolrHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (UshSolrHarvesterConfig UshSolrHarvesterConfig: harvesters ) {
                dataProvider.getList().add(UshSolrHarvesterConfig);
            }
        }
        Collections.sort(dataProvider.getList(), (o1, o2) -> o1.getContent().getUshHarvesterJobId().compareTo(o2.getContent().getUshHarvesterJobId()));
    }


    /*
     * Local methods
     * /

    /**
     * This method constructs the JobId column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed JobId column
     */
    private Column constructJobIdColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return String.valueOf(harvester.getContent().getUshHarvesterJobId());
            }
        };
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    private Column constructNameColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return harvester.getContent().getName();
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
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return harvester.getContent().getUshHarvesterProperties().getCurrentStatus();
            }
        };
    }

    /**
     * This method constructs the LatestHarvest column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed LatestHarvest column
     */
    private Column constructLatestHarvestColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return Format.formatLongDate(harvester.getContent().getUshHarvesterProperties().getLastHarvestFinished());
            }
        };
    }

    /**
     * This method constructs the Count column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Count column
     */
    private Column constructCountColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return String.valueOf(harvester.getContent().getUshHarvesterProperties().getAmountHarvested());
            }
        };
    }

    /**
     * This method constructs the NextHarverst column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed NextHarverst column
     */
    private Column constructNextHarverstColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return Format.formatLongDate(harvester.getContent().getUshHarvesterProperties().getNextHarvestSchedule());
            }
        };
    }

    /**
     * This method constructs the StatusMessage column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed StatusMessage column
     */
    private Column constructStatusMessageColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return harvester.getContent().getUshHarvesterProperties().getMessage();
            }
        };
    }

    /**
     * This method constructs the Enabled column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Enabled column
     */
    private Column constructEnabledColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return harvester.getContent().getUshHarvesterProperties().getEnabled() ? texts.value_Enabled() : texts.value_Disabled();
            }
        };
    }

    /**
     * This method constructs the Action column
     * @return The constructed Action column
     */
    private Column constructActionColumn() {
        Column column = new Column<UshSolrHarvesterConfig, String>(new ButtonCell()) {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };
        column.setFieldUpdater(new FieldUpdater<UshSolrHarvesterConfig, String>() {
            @Override
            public void update(int index, UshSolrHarvesterConfig config, String buttonText) {
                editUshHarvester(config);
            }
        });
        return column;
    }

    /**
     * This metods constructs a SafeHtml snippet, that constitutes a text with a popup mouseover help text
     * @param headerText The headertext to be displayed
     * @param helpText The popup help text
     * @return The SafeHtml snippet
     */
    SafeHtml textWithToolTip(String headerText, String helpText) {
        return SafeHtmlUtils.fromSafeConstant("<span title='" + helpText + "'>" + headerText + "</span>");
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler(){
        return doubleClickEvent -> editUshHarvester(selectionModel.getSelectedObject());
    }

    private void editUshHarvester(UshSolrHarvesterConfig harvester) {
        if (harvester != null) {
            presenter.editHarvesterConfig(String.valueOf(harvester.getId()));
        }
    }

}
