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
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
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
    private final String LINE_BREAK = "<br>";

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

        addColumn(constructJobIdColumn(), textWithToolTip(texts.columnHeader_JobId()));
        addColumn(constructNameColumn(), textWithToolTip(texts.columnHeader_Name()));
        addColumn(constructDescriptionColumn(), textWithToolTip(texts.columnHeader_Description()));
        addColumn(constructStatusColumn(), textWithToolTip(texts.columnHeader_Status()));
        addColumn(constructLatestHarvestColumn(), textWithToolTip(texts.columnHeader_LatestHarvest()));
        addColumn(constructCountColumn(), textWithToolTip(texts.columnHeader_Count()));
        addColumn(constructNextHarverstColumn(), textWithToolTip(texts.columnHeader_NextHarverst()));
        addColumn(constructStatusMessageColumn(), textWithToolTip(texts.columnHeader_StatusMessage()));
        addColumn(constructSubmitterColumn(), textWithToolTip(texts.columnHeader_Submitter()));
        addColumn(constructFormatColumn(), textWithToolTip(texts.columnHeader_Format()));
        addColumn(constructDestinationColumn(), textWithToolTip(texts.columnHeader_Destination()));
        addColumn(constructEnabledColumn(), textWithToolTip(texts.columnHeader_Enabled()));
        addColumn(constructActionColumn(), textWithToolTip(texts.columnHeader_Action()));

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
     * This method constructs the Description column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Description column
     */
    private Column constructDescriptionColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return harvester.getContent().getDescription();
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
        return new Column<UshSolrHarvesterConfig, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(UshSolrHarvesterConfig harvester) {
                return twoLiner(true,
                        Format.formatLongDate(harvester.getContent().getUshHarvesterProperties().getLastHarvestFinished()),
                        Format.formatLongDate(harvester.getContent().getTimeOfLastHarvest())
                );
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
     * This method constructs the Submitter column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Submitter column
     */
    private Column constructSubmitterColumn() {
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                Integer submitter = harvester.getContent().getSubmitterNumber();
                return submitter == null ? "" : String.valueOf(submitter);
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
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return harvester.getContent().getFormat();
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
        return new TextColumn<UshSolrHarvesterConfig>() {
            @Override
            public String getValue(UshSolrHarvesterConfig harvester) {
                return harvester.getContent().getDestination();
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
        return new Column<UshSolrHarvesterConfig, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(UshSolrHarvesterConfig harvester) {
                return twoLiner(false,
                        harvester.getContent().getUshHarvesterProperties().getEnabled() ? texts.value_Enabled() : texts.value_Disabled(),
                        harvester.getContent().isEnabled() ? texts.value_Enabled() : texts.value_Disabled()
                );
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
     * @return The SafeHtml snippet
     */
    SafeHtml textWithToolTip(String headerText) {
        return SafeHtmlUtils.fromSafeConstant("<span title='" + texts.help_ColumnHeader() + "'>" + headerText + "</span>");
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler(){
        return doubleClickEvent -> editUshHarvester(selectionModel.getSelectedObject());
    }

    /**
     * Sends a request to the presenter for editing the harvester, passed as a parameter in the call
     * @param harvester The harvester to edit
     */
    private void editUshHarvester(UshSolrHarvesterConfig harvester) {
        if (harvester != null) {
            presenter.editHarvesterConfig(String.valueOf(harvester.getId()));
        }
    }

    /**
     * Constructs a two line representation of the two strings, passed as parameters in the call
     * @param pre A boolean determining, whether a pre-text should be included for the two lines
     * @param first First string
     * @param second Second string
     * @return The constructed two liner representation of the two strings
     */
    private SafeHtml twoLiner(Boolean pre, String first, String second) {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        if (pre) {
            sb.appendHtmlConstant(texts.pre_First());
        }
        sb.appendEscaped(first);
        sb.appendHtmlConstant(LINE_BREAK);
        if (pre) {
            sb.appendHtmlConstant(texts.pre_Second());
        }
        sb.appendEscaped(second);
        return sb.toSafeHtml();
    }

}
