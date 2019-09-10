/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.commons.types.jndi.RawRepo;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

import java.util.Comparator;
import java.util.List;


/**
 * Harvesters Table for the Harvester View
 */
public class HarvestersTable extends CellTable {
    private ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<PeriodicJobsHarvesterConfig> dataProvider;

    public HarvestersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);
        addColumn(constructNameColumn(), texts.columnHeader_Name());
        addColumn(constructScheduleColumn(), textWithToolTip(texts.columnHeader_Schedule(), texts.help_Schedule()));
        addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        addColumn(constructResourceColumn(), textWithToolTip(texts.columnHeader_Resource(), texts.help_Resource()));
        addColumn(constructCollectionColumn(), textWithToolTip(texts.columnHeader_Collection(), texts.help_Collection()));
        addColumn(constructDestinationColumn(), texts.columnHeader_Destination());
        addColumn(constructFormatColumn(), texts.columnHeader_Format());
        addColumn(constructSubmitterColumn(), texts.columnHeader_SubmitterNumber());
        addColumn(constructTimeOfLastHarvestColumn(), texts.columnHeader_TimeOfLastHarvest());
        addColumn(constructStatusColumn(), texts.columnHeader_Status());
    }

    /**
     * This method sets the harvester data for the table
     * @param presenter The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<PeriodicJobsHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (PeriodicJobsHarvesterConfig config : harvesters) {
                dataProvider.getList().add(config);
            }
        }
        (dataProvider.getList()).sort(Comparator.comparing(o -> o.getContent().getName()));
    }

    private Column constructNameColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().getName();
            }
        };
    }

    private Column constructScheduleColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().getSchedule();
            }
        };
    }

    private Column constructDescriptionColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().getDescription();
            }
        };
    }

    private Column constructResourceColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                final RawRepo rawRepo = RawRepo.fromString(config.getContent().getResource());
                if (rawRepo != null) {
                    return rawRepo.name().toLowerCase();
                }
                return "";
            }
        };
    }

    private Column constructCollectionColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().getCollection();
            }
        };
    }

    private Column constructDestinationColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().getDestination();
            }
        };
    }

    private Column constructFormatColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().getFormat();
            }
        };
    }

    private Column constructSubmitterColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().getSubmitterNumber();
            }
        };
    }

    private Column constructTimeOfLastHarvestColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return Format.formatLongDate(config.getContent().getTimeOfLastHarvest());
            }
        };
    }

    private Column constructStatusColumn() {
        return new TextColumn<PeriodicJobsHarvesterConfig>() {
            @Override
            public String getValue(PeriodicJobsHarvesterConfig config) {
                return config.getContent().isEnabled()
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
