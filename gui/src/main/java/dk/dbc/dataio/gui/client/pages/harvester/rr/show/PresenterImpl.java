package dk.dbc.dataio.gui.client.pages.harvester.rr.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.pages.harvester.rr.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.harvester.rr.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;

import java.util.List;


/**
 * This class represents the show harvesters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private PlaceController placeController;

    /**
     * Default constructor
     *
     * @param placeController The place controller
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
        getView().setHeader(commonInjector.getMenuTexts().menu_RrHarvesters());
        containerWidget.setWidget(getView().asWidget());
        fetchHarvesters();
    }


    /*
     * Overridden methods
     */

    /**
     * This method starts the edit harvester page
     *
     * @param id The id of the harvester configuration to edit
     */
    @Override
    public void editHarvesterConfig(String id) {
        this.placeController.goTo(new EditPlace(id));
    }

    /**
     * This method starts the create harvester page
     */
    @Override
    public void createHarvester() {
        this.placeController.goTo(new CreatePlace());
    }

    /**
     * This method fetches all harvesters, and sends them to the view
     */
    private void fetchHarvesters() {
        commonInjector.getFlowStoreProxyAsync().findAllRRHarvesterConfigs(new GetHarvestersCallback());
    }

    private View getView() {
        return viewInjector.getView();
    }

    protected class GetHarvestersCallback extends FilteredAsyncCallback<List<RRHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(caught, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<RRHarvesterConfig> rrHarvesterConfigs) {
            getView().setHarvesters(rrHarvesterConfigs);
        }
    }
}
