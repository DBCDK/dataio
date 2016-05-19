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

package dk.dbc.dataio.gui.client.pages.harvester.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Harvesters Table for the Harvester View
 */
public class HarvestersTable extends CellTable {
    ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    ListDataProvider<Entry> dataProvider;
    SingleSelectionModel<Entry> selectionModel = new SingleSelectionModel<>();

    /**
     * Constructor
     */
    public HarvestersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructNameColumn(), textWithToolTip(texts.columnHeader_Name(), texts.help_Name()));
        addColumn(constructResourceColumn(), textWithToolTip(texts.columnHeader_Resource(), texts.help_Resource()));
        addColumn(constructTargetColumn(), textWithToolTip(texts.columnHeader_Target(), texts.help_Target()));
        addColumn(constructConsumerIdColumn(), textWithToolTip(texts.columnHeader_Id(), texts.help_Id()));
        addColumn(constructSizeColumn(), textWithToolTip(texts.columnHeader_Size(), texts.help_Size()));
        addColumn(constructFormatOverridesColumn(), textWithToolTip(texts.columnHeader_FormatOverrides(), texts.help_FormatOverrides()));
        addColumn(constructRelationsColumn(), textWithToolTip(texts.columnHeader_Relations(), texts.help_Relations()));
        addColumn(constructDestinationColumn(), textWithToolTip(texts.columnHeader_Destination(), texts.help_Destination()));
        addColumn(constructFormatColumn(), textWithToolTip(texts.columnHeader_Format(), texts.help_Format()));
        addColumn(constructTypeColumn(), textWithToolTip(texts.columnHeader_Type(), texts.help_Type()));

        setSelectionModel(selectionModel);
    }


    /**
     * This method sets the harvester data for the table
     * @param harvesters The harvester data
     */
    public void setHarvesters(RawRepoHarvesterConfig harvesters) {
        dataProvider.getList().clear();
        if (!harvesters.getEntries().isEmpty()) {
            for (Entry entry: harvesters.getEntries()) {
                dataProvider.getList().add(entry);
            }
        }
        Collections.sort(dataProvider.getList(), (o1, o2) -> o1.getId().compareTo(o2.getId()));
    }


    /*
     * Local methods
     */

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    private Column constructNameColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getId();
            }
        };
    }

    /**
     * This method constructs the Resource column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Resource column
     */
    private Column constructResourceColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getResource();
            }
        };
    }

    /**
     * This method constructs the Target column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Target column
     */
    private Column constructTargetColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getOpenAgencyTarget().getUrl();
            }
        };
    }

    /**
     * This method constructs the ConsumerId column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed ConsumerId column
     */
    private Column constructConsumerIdColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getConsumerId();
            }
        };
    }

    /**
     * This method constructs the Size column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Size column
     */
    private Column constructSizeColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return String.valueOf(harvester.getBatchSize());
            }
        };
    }

    /**
     * This method constructs the FormatOverrides column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed FormatOverrides column
     */
    private Column constructFormatOverridesColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                List<String> formats = new ArrayList<>();
                for (Map.Entry<Integer, String> entry : harvester.getFormatOverrides().entrySet()) {
                    formats.add(entry.getKey().toString() + " - " + entry.getValue());
                }
                Collections.sort(formats);
                return Format.commaSeparate(formats);
            }
        };
    }

    /**
     * This method constructs the Relations column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Relations column
     */
    private Column constructRelationsColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.includeRelations() ? texts.includeRelationsTrue() : texts.includeRelationsFalse();
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
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getDestination();
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
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getFormat();
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
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getType().toString();
            }
        };
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


}
