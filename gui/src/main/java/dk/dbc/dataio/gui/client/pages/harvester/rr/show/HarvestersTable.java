package dk.dbc.dataio.gui.client.pages.harvester.rr.show;

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
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

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
    Presenter presenter;
    ListDataProvider<RRHarvesterConfig> dataProvider;
    SingleSelectionModel<RRHarvesterConfig> selectionModel = new SingleSelectionModel<>();

    /**
     * Constructor
     */
    public HarvestersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructNameColumn(), textWithToolTip(texts.columnHeader_Name(), texts.help_Name()));
        addColumn(constructDescriptionColumn(), textWithToolTip(texts.columnHeader_Description(), texts.help_Description()));
        addColumn(constructResourceColumn(), textWithToolTip(texts.columnHeader_Resource(), texts.help_Resource()));
        addColumn(constructConsumerIdColumn(), textWithToolTip(texts.columnHeader_Id(), texts.help_Id()));
        addColumn(constructSizeColumn(), textWithToolTip(texts.columnHeader_Size(), texts.help_Size()));
        addColumn(constructFormatOverridesColumn(), textWithToolTip(texts.columnHeader_FormatOverrides(), texts.help_FormatOverrides()));
        addColumn(constructRelationsColumn(), textWithToolTip(texts.columnHeader_Relations(), texts.help_Relations()));
        addColumn(constructExpandColumn(), textWithToolTip(texts.columnHeader_Expand(), texts.help_Expand()));
        addColumn(constructLibraryRulesColumn(), textWithToolTip(texts.columnHeader_LibraryRules(), texts.help_LibraryRules()));
        addColumn(constructHarvesterTypeColumn(), textWithToolTip(texts.columnHeader_HarvesterType(), texts.help_HarvesterType()));
        addColumn(constructHoldingsTargetColumn(), textWithToolTip(texts.columnHeader_HoldingsTarget(), texts.help_HoldingsTarget()));
        addColumn(constructDestinationColumn(), textWithToolTip(texts.columnHeader_Destination(), texts.help_Destination()));
        addColumn(constructFormatColumn(), textWithToolTip(texts.columnHeader_Format(), texts.help_Format()));
        addColumn(constructTypeColumn(), textWithToolTip(texts.columnHeader_Type(), texts.help_Type()));
        addColumn(constructStatusColumn(), textWithToolTip(texts.columnHeader_Status(), texts.help_Status()));
        addColumn(constructActionColumn(), textWithToolTip(texts.columnHeader_Action(), texts.help_Action()));

        setSelectionModel(selectionModel);
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }


    /**
     * This method sets the harvester data for the table
     *
     * @param presenter  The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<RRHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (RRHarvesterConfig RRHarvesterConfig : harvesters) {
                dataProvider.getList().add(RRHarvesterConfig);
            }
        }
        Collections.sort(dataProvider.getList(), (o1, o2) -> o1.getContent().getId().compareTo(o2.getContent().getId()));
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().getId();
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().getDescription();
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().getResource();
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().getConsumerId();
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return String.valueOf(harvester.getContent().getBatchSize());
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                List<String> formats = new ArrayList<>();
                for (Map.Entry<Integer, String> FormatOverWrite : harvester.getContent().getFormatOverrides().entrySet()) {
                    formats.add(FormatOverWrite.getKey().toString() + " - " + FormatOverWrite.getValue());
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().hasIncludeRelations() ? texts.value_FlagTrue() : texts.value_FlagFalse();
            }
        };
    }

    private Column constructExpandColumn() {
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().expand() ? texts.value_FlagTrue() : texts.value_FlagFalse();
            }
        };
    }

    /**
     * This method constructs the Library Rules column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Library Rules column
     */
    private Column constructLibraryRulesColumn() {
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().hasIncludeLibraryRules() ? texts.value_FlagTrue() : texts.value_FlagFalse();
            }
        };
    }

    /**
     * This method constructs the IMS Harvester column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed IMS Harvester column
     */
    private Column constructHarvesterTypeColumn() {
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().getHarvesterType().toString();
            }
        };
    }

    /**
     * This method constructs the Holdings Target column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Holdings Target column
     */
    private Column constructHoldingsTargetColumn() {
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().getImsHoldingsTarget();
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
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
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().getType().toString();
            }
        };
    }

    /**
     * This method constructs the Status column
     *
     * @return the constructed Status column
     */
    private Column constructStatusColumn() {
        return new TextColumn<RRHarvesterConfig>() {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                return harvester.getContent().isEnabled() ? texts.harvesterEnabled() : texts.harvesterDisabled();
            }
        };
    }

    /**
     * This method constructs the Action column
     *
     * @return The constructed Action column
     */
    private Column constructActionColumn() {
        Column column = new Column<RRHarvesterConfig, String>(new ButtonCell()) {
            @Override
            public String getValue(RRHarvesterConfig harvester) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };
        column.setFieldUpdater(new FieldUpdater<RRHarvesterConfig, String>() {
            @Override
            public void update(int index, RRHarvesterConfig config, String buttonText) {
                presenter.editHarvesterConfig(String.valueOf(config.getId()));
            }
        });
        return column;
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

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> {
            RRHarvesterConfig selected = selectionModel.getSelectedObject();
            if (selected != null) {
                presenter.editHarvesterConfig(String.valueOf(selected.getId()));
            }
        };
    }

}
