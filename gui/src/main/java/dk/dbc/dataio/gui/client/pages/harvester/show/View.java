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


import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.RawRepoHarvesterConfig.Entry;

import java.util.Collections;
import java.util.Comparator;

/**
 * This class is the View class for the Harvesters Show View
 */
public class View extends ViewWidget {
    ListDataProvider<Entry> dataProvider;
    SingleSelectionModel<Entry> selectionModel = new SingleSelectionModel<>();

    public View() {
        super("");
        setupColumns();
    }

    /**
     * This method is used to put data into the view
     *
     * @param harvesters The list of submitters to put into the view
     */
    public void setHarvesters(RawRepoHarvesterConfig harvesters) {
        dataProvider.getList().clear();
        if (!harvesters.getEntries().isEmpty()) {
            for (Entry entry: harvesters.getEntries()) {
                dataProvider.getList().add(entry);
            }
        }
        Collections.sort(dataProvider.getList(), new Comparator<Entry>() {
            @Override
            public int compare(Entry o1, Entry o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });

    }

    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setSubmitters method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(harvestersTable);

        harvestersTable.addColumn(constructNameColumn(), textWithToolTip(getTexts().columnHeader_Name(), getTexts().help_Name()));
        harvestersTable.addColumn(constructResourceColumn(), textWithToolTip(getTexts().columnHeader_Resource(), getTexts().help_Resource()));
        harvestersTable.addColumn(constructTargetColumn(), textWithToolTip(getTexts().columnHeader_Target(), getTexts().help_Target()));
        harvestersTable.addColumn(constructConsumerIdColumn(), textWithToolTip(getTexts().columnHeader_Id(), getTexts().help_Id()));
        harvestersTable.addColumn(constructSizeColumn(), textWithToolTip(getTexts().columnHeader_Size(), getTexts().help_Size()));
        harvestersTable.addColumn(constructFormatOverridesColumn(), textWithToolTip(getTexts().columnHeader_FormatOverrides(), getTexts().help_FormatOverrides()));
        harvestersTable.addColumn(constructRelationsColumn(), textWithToolTip(getTexts().columnHeader_Relations(), getTexts().help_Relations()));
        harvestersTable.addColumn(constructDestinationColumn(), textWithToolTip(getTexts().columnHeader_Destination(), getTexts().help_Destination()));
        harvestersTable.addColumn(constructFormatColumn(), textWithToolTip(getTexts().columnHeader_Format(), getTexts().help_Format()));
        harvestersTable.addColumn(constructTypeColumn(), textWithToolTip(getTexts().columnHeader_Type(), getTexts().help_Type()));

        harvestersTable.setSelectionModel(selectionModel);
    }

    /**
     * This method constructs the Name column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Name column
     */
    Column constructNameColumn() {
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
    Column constructResourceColumn() {
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
    Column constructTargetColumn() {
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
    Column constructConsumerIdColumn() {
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
    Column constructSizeColumn() {
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
    Column constructFormatOverridesColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return "870970 - " + harvester.getFormat(870970) +  " ???";
            }
        };
    }

    /**
     * This method constructs the Relations column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Relations column
     */
    Column constructRelationsColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.includeRelations() ? getTexts().includeRelationsTrue() : getTexts().includeRelationsFalse();
            }
        };
    }

    /**
     * This method constructs the Destination column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Destination column
     */
    Column constructDestinationColumn() {
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
    Column constructFormatColumn() {
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
    Column constructTypeColumn() {
        return new TextColumn<Entry>() {
            @Override
            public String getValue(Entry harvester) {
                return harvester.getType().toString();
            }
        };
    }


    /**
     * Private methods
     */

    /**
     * This metods constructs a SafeHtml snippet, that constitutes a text with a popup mouseover help text
     * @param headerText The headertext to be displayed
     * @param helpText The popup help text
     * @return The SafeHtml snippet
     */
    private SafeHtml textWithToolTip(String headerText, String helpText) {
        return SafeHtmlUtils.fromSafeConstant("<span title='" + helpText + "'>" + headerText + "</span>");
    }


}
