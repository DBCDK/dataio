/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.infomedia.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;

import java.util.Comparator;
import java.util.List;


/**
 * Harvesters Table for the Harvester View
 */
public class HarvestersTable extends CellTable {
    private ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<InfomediaHarvesterConfig> dataProvider;

    public HarvestersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);
        addColumn(constructIdColumn(), texts.columnHeader_Id());
        addColumn(constructScheduleColumn(), textWithToolTip(texts.columnHeader_Schedule(), texts.help_Schedule()));
        addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        addColumn(constructDestinationColumn(), texts.columnHeader_Destination());
        addColumn(constructFormatColumn(), texts.columnHeader_Format());
        addColumn(constructTimeOfLastHarvestColumn(), texts.columnHeader_TimeOfLastHarvest());
        addColumn(constructNextPublicationDateColumn(), texts.columnHeader_NextPublicationDate());
        addColumn(constructStatusColumn(), texts.columnHeader_Status());
    }

    /**
     * This method sets the harvester data for the table
     * @param presenter The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<InfomediaHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (InfomediaHarvesterConfig infomediaHarvesterConfig : harvesters) {
                dataProvider.getList().add(infomediaHarvesterConfig);
            }
        }
        (dataProvider.getList()).sort(Comparator.comparing(o -> o.getContent().getId()));
    }

    /**
     * @return publication ID column
     */
    private Column constructIdColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return harvester.getContent().getId();
            }
        };
    }

    /**
     * @return harvest schedule column
     */
    private Column constructScheduleColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return harvester.getContent().getSchedule();
            }
        };
    }

    /**
     * @return description column
     */
    private Column constructDescriptionColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return harvester.getContent().getDescription();
            }
        };
    }

    /**
     * @return destination column
     */
    private Column constructDestinationColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return harvester.getContent().getDestination();
            }
        };
    }

    /**
     * @return the format column
     */
    private Column constructFormatColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return harvester.getContent().getFormat();
            }
        };
    }

    /**
     * @return time of last harvest column
     */
    private Column constructTimeOfLastHarvestColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return Format.formatLongDate(harvester.getContent().getTimeOfLastHarvest());
            }
        };
    }

    /**
     * @return time of last harvest column
     */
    private Column constructNextPublicationDateColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return Format.formatLongDate(harvester.getContent().getNextPublicationDate());
            }
        };
    }

    /**
     * @return status column
     */
    private Column constructStatusColumn() {
        return new TextColumn<InfomediaHarvesterConfig>() {
            @Override
            public String getValue(InfomediaHarvesterConfig harvester) {
                return harvester.getContent().isEnabled()
                        ? texts.value_Enabled() : texts.value_Disabled();
            }
        };
    }

    /**
     * Constructs a SafeHtml snippet constituting a text with a popup mouseover help text
     * @param headerText header text to be displayed
     * @param helpText popup help text
     * @return SafeHtml snippet
     */
    SafeHtml textWithToolTip(String headerText, String helpText) {
        return SafeHtmlUtils.fromSafeConstant("<span title='" + helpText + "'>" + headerText + "</span>");
    }
}