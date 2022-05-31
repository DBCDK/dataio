package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

import java.util.List;


/**
 * This class represents the show harvesters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    PlaceController placeController;


    /**
     * Default constructor
     *
     * @param placeController The placecontroller
     */
    public PresenterImpl(PlaceController placeController) {
        this.placeController = placeController;
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setPresenter(this);
        getView().setHeader(commonInjector.getMenuTexts().menu_TickleHarvesters());
        containerWidget.setWidget(getView().asWidget());
        fetchHarvesters();
    }


    /*
     * Overridden interface methods
     */

    /**
     * This method starts the edit harvester page
     *
     * @param id The id of the harvester configuration to edit
     */
    @Override
    public void editTickleRepoHarvesterConfig(String id) {
        this.placeController.goTo(new EditPlace(id));
    }

    /**
     * This method starts the create harvester page
     */
    @Override
    public void createTickleRepoHarvester() {
        placeController.goTo(new CreatePlace());
    }

    /**
     * This method fetches all harvesters, and sends them to the view
     */
    private void fetchHarvesters() {
        commonInjector.getFlowStoreProxyAsync().findAllTickleRepoHarvesterConfigs(new GetTickleRepoHarvestersCallback());
    }

    private View getView() {
        return viewInjector.getView();
    }

    class GetTickleRepoHarvestersCallback extends FilteredAsyncCallback<List<TickleRepoHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(caught, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<TickleRepoHarvesterConfig> tickleRepoHarvesterConfigs) {
            getView().setHarvesters(tickleRepoHarvesterConfigs);
        }
    }
}
