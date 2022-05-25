package dk.dbc.dataio.gui.client.pages.harvester.corepo.show;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Harvesters Table for the Harvester View
 */
public class HarvestersTable extends CellTable {
    private ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    private CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private View view;
    Texts texts = viewGinjector.getTexts();
    Presenter presenter;
    ListDataProvider<CoRepoHarvesterConfig> dataProvider;
    SingleSelectionModel<CoRepoHarvesterConfig> selectionModel = new SingleSelectionModel<>();

    private final Map<Long, RRHarvesterConfig> configMap = new HashMap<>();

    /**
     * Constructor
     *
     * @param view The View, holding the Harvesters Table
     */
    public HarvestersTable(View view) {
        this.view = view;
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        addColumn(constructNameColumn(), texts.columnHeader_Name());
        addColumn(constructDescriptionColumn(), texts.columnHeader_Description());
        addColumn(constructResourceColumn(), texts.columnHeader_Resource());
        addColumn(constructRrHarvesterColumn(), texts.columnHeader_RrHarvester());
        addColumn(constructTimeOfLastHarvestColumn(), texts.columnHeader_TimeOfLastHarvest());
        addColumn(constructStatusColumn(), texts.columnHeader_Status());
        addColumn(constructActionColumn(), texts.columnHeader_Action());

        setSelectionModel(selectionModel);
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
        commonInjector.getFlowStoreProxyAsync().findAllRRHarvesterConfigs(new FetchAvailableRRHarvesterConfigsCallback());
    }


    /**
     * This method sets the harvester data for the table
     *
     * @param presenter  The presenter
     * @param harvesters The harvester data
     */
    public void setHarvesters(Presenter presenter, List<CoRepoHarvesterConfig> harvesters) {
        this.presenter = presenter;
        dataProvider.getList().clear();

        if (!harvesters.isEmpty()) {
            for (CoRepoHarvesterConfig CoRepoHarvesterConfig : harvesters) {
                dataProvider.getList().add(CoRepoHarvesterConfig);
            }
        }
        (dataProvider.getList()).sort(Comparator.comparing(o -> o.getContent().getName()));
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
        return new TextColumn<CoRepoHarvesterConfig>() {
            @Override
            public String getValue(CoRepoHarvesterConfig harvester) {
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
        return new TextColumn<CoRepoHarvesterConfig>() {
            @Override
            public String getValue(CoRepoHarvesterConfig harvester) {
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
        return new TextColumn<CoRepoHarvesterConfig>() {
            @Override
            public String getValue(CoRepoHarvesterConfig harvester) {
                return harvester.getContent().getResource();
            }
        };
    }

    /**
     * This method constructs the Resource column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Resource column
     */
    private Column constructRrHarvesterColumn() {
        return new TextColumn<CoRepoHarvesterConfig>() {
            @Override
            public String getValue(CoRepoHarvesterConfig config) {
                long rrHarvester = config.getContent().getRrHarvester();
                if (configMap.containsKey(rrHarvester)) {
                    return configMap.get(rrHarvester).getContent().getId();
                }
                return "";
            }
        };
    }

    /**
     * This method constructs the TimeOfLastHarvest column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed TimeOfLastHarvest column
     */
    private Column constructTimeOfLastHarvestColumn() {
        return new TextColumn<CoRepoHarvesterConfig>() {
            @Override
            public String getValue(CoRepoHarvesterConfig harvester) {
                return harvester.getContent().getTimeOfLastHarvest() == null ? "" : Format.formatLongDate(harvester.getContent().getTimeOfLastHarvest());
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
        return new TextColumn<CoRepoHarvesterConfig>() {
            @Override
            public String getValue(CoRepoHarvesterConfig harvester) {
                return harvester.getContent().isEnabled() ? texts.value_Enabled() : texts.value_Disabled();
            }
        };
    }

    /**
     * This method constructs the Action column
     *
     * @return The constructed Action column
     */
    private Column constructActionColumn() {
        Column column = new Column<CoRepoHarvesterConfig, String>(new ButtonCell()) {
            @Override
            public String getValue(CoRepoHarvesterConfig harvester) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };
        column.setFieldUpdater((index, config, buttonText) -> editCoRepoHarvester((CoRepoHarvesterConfig) config));
        return column;
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> editCoRepoHarvester(selectionModel.getSelectedObject());
    }

    /**
     * Sends a request to the presenter for editing the harvester, passed as a parameter in the call
     *
     * @param harvester The harvester to edit
     */
    private void editCoRepoHarvester(CoRepoHarvesterConfig harvester) {
        if (harvester != null) {
            presenter.editCoRepoHarvesterConfig(String.valueOf(harvester.getId()));
        }
    }


    /*
     * Local classes
     */

    /**
     * Local call back class to be instantiated in the call to findAllRRHarvesterConfigs in flowstore proxy
     */
    class FetchAvailableRRHarvesterConfigsCallback extends FilteredAsyncCallback<List<RRHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<RRHarvesterConfig> configs) {
            configMap.clear();
            if (configs != null) {
                for (RRHarvesterConfig config : configs) {
                    configMap.put(config.getId(), config);
                }
                setVisibleRangeAndClearData(getVisibleRange(), true);  // Now reload data to display RR Harvester names
            }
        }
    }

}
