package dk.dbc.dataio.gui.client.pages.harvester.dmat.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;

import java.util.Comparator;
import java.util.List;


/**
 * Harvesters Table for the Harvester View
 */
public class HarvestersTable extends CellTable {
    private ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<DMatHarvesterConfig> dataProvider;
    SingleSelectionModel<DMatHarvesterConfig> selectionModel = new SingleSelectionModel<>();

    public HarvestersTable() {
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);
        addColumn(constructNameColumn(), texts.columnHeader_Name());
        addColumn(constructScheduleColumn(), textWithToolTip(texts.columnHeader_Schedule(), texts.help_Schedule()));
        addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        addColumn(constructDestinationColumn(), texts.columnHeader_Destination());
        addColumn(constructFormatColumn(), texts.columnHeader_Format());
        addColumn(constructTimeOfLastHarvestColumn(), texts.columnHeader_TimeOfLastHarvest());
        addColumn(constructStatusColumn(), texts.columnHeader_Status());
        addColumn(constructActionColumn(), texts.columnHeader_Action());

        setSelectionModel(selectionModel);
        addDomHandler(doubleClickEvent -> editConfig(), DoubleClickEvent.getType());
    }

    /**
     * This method sets the harvester data for the table
     *
     * @param presenter  The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<DMatHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (DMatHarvesterConfig config : harvesters) {
                dataProvider.getList().add(config);
            }
        }
        (dataProvider.getList()).sort(Comparator.comparing(o -> o.getContent().getName()));
    }

    private Column constructNameColumn() {
        return new TextColumn<DMatHarvesterConfig>() {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                return config.getContent().getName();
            }
        };
    }

    private Column constructScheduleColumn() {
        return new TextColumn<DMatHarvesterConfig>() {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                return config.getContent().getSchedule();
            }
        };
    }

    private Column constructDescriptionColumn() {
        return new TextColumn<DMatHarvesterConfig>() {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                return config.getContent().getDescription();
            }
        };
    }

    private Column constructDestinationColumn() {
        return new TextColumn<DMatHarvesterConfig>() {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                return config.getContent().getDestination();
            }
        };
    }

    private Column constructFormatColumn() {
        return new TextColumn<DMatHarvesterConfig>() {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                return config.getContent().getFormat();
            }
        };
    }

    private Column constructTimeOfLastHarvestColumn() {
        return new TextColumn<DMatHarvesterConfig>() {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                return Format.formatLongDate(config.getContent().getTimeOfLastHarvest());
            }
        };
    }

    private Column constructStatusColumn() {
        return new TextColumn<DMatHarvesterConfig>() {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                return config.getContent().isEnabled()
                        ? texts.value_Enabled() : texts.value_Disabled();
            }
        };
    }

    private Column constructActionColumn() {
        Column column = new Column<DMatHarvesterConfig, String>(new ButtonCell()) {
            @Override
            public String getValue(DMatHarvesterConfig config) {
                // The value to display in the button.
                return texts.button_EditDMatHarvesterButton();
            }
        };
        column.setFieldUpdater(
                (FieldUpdater<DMatHarvesterConfig, String>)
                        (index, config, buttonText) -> editConfig(config));
        return column;
    }

    /**
     * Constructs a SafeHtml snippet constituting a text with a popup mouseover help text
     *
     * @param headerText header text to be displayed
     * @param helpText   popup help text
     * @return SafeHtml snippet
     */
    SafeHtml textWithToolTip(String headerText, String helpText) {
        return SafeHtmlUtils.fromSafeConstant("<span title='" + helpText + "'>" + headerText + "</span>");
    }

    /**
     * Sends a request to the presenter for editing the selected harvester
     */
    private void editConfig() {
        editConfig(selectionModel.getSelectedObject());
    }

    private void editConfig(DMatHarvesterConfig config) {
        if (config != null) {
            presenter.editDMatHarvester(String.valueOf(config.getId()));
        }
    }
}
