package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show;

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
        addColumn(constructTimeOfLastBatchHarvestColumn(), textWithToolTip(texts.columnHeader_TimeOfLastBatchHarvest(), texts.help_batchId()));
        addColumn(constructStatusColumn(), texts.columnHeader_Status());
        addColumn(constructNotificationsColumn(), texts.columnHeader_Notifications());
        addColumn(constructActionColumn(), texts.columnHeader_Action());

        setSelectionModel(selectionModel);
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }


    /**
     * This method sets the harvester data for the table
     *
     * @param presenter  The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<TickleRepoHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (TickleRepoHarvesterConfig TickleRepoHarvesterConfig : harvesters) {
                dataProvider.getList().add(TickleRepoHarvesterConfig);
            }
        }
        (dataProvider.getList()).sort(Comparator.comparing(o -> o.getContent().getId()));
    }


    /*
     * Local methods
     */

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
     * This method constructs the TimeOfLastHarvest column
     *
     * @return the constructed TimeOfLastHarvest column
     */
    private Column constructTimeOfLastBatchHarvestColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return buildCommaSeperatedHarvestBatchTimeStamp(harvester.getContent());
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

    private Column constructNotificationsColumn() {
        return new TextColumn<TickleRepoHarvesterConfig>() {
            @Override
            public String getValue(TickleRepoHarvesterConfig harvester) {
                return harvester.getContent().hasNotificationsEnabled()
                        ? texts.value_Enabled() : texts.value_Disabled();
            }
        };
    }

    /**
     * This method constructs the Action column
     *
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
     *
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> editTickleRepoHarvester(selectionModel.getSelectedObject());
    }

    /**
     * Sends a request to the presenter for editing the harvester, passed as a parameter in the call
     *
     * @param harvester The harvester to edit
     */
    private void editTickleRepoHarvester(TickleRepoHarvesterConfig harvester) {
        if (harvester != null) {
            presenter.editTickleRepoHarvesterConfig(String.valueOf(harvester.getId()));
        }
    }

    private String buildCommaSeperatedHarvestBatchTimeStamp(TickleRepoHarvesterConfig.Content content) {
        StringBuilder stringBuilder = new StringBuilder();
        if (content.getTimeOfLastBatchHarvested() != null) {
            stringBuilder.append(Format.formatLongDate(content.getTimeOfLastBatchHarvested())).append(", ");
        } else {
            stringBuilder.append("na, ");
        }
        stringBuilder.append(content.getLastBatchHarvested());
        return stringBuilder.toString();
    }

    /**
     * This metods constructs a SafeHtml snippet, that constitutes a text with a popup mouseover help text
     *
     * @param headerText The headertext to be displayed
     * @param helpText   The popup help text
     * @return The SafeHtml snippet
     */
    SafeHtml textWithToolTip(String headerText, String helpText) {
        return SafeHtmlUtils.fromSafeConstant("<span title='" + helpText + "'>" + headerText + "</span>");
    }


}
