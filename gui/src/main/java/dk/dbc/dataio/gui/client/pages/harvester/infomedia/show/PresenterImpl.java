package dk.dbc.dataio.gui.client.pages.harvester.infomedia.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;

import java.util.List;

/**
 * This class represents the show harvesters presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    PlaceController placeController;

    public PresenterImpl(PlaceController placeController) {
        this.placeController = placeController;
    }

    /**
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setPresenter(this);
        getView().setHeader(commonInjector.getMenuTexts().menu_InfomediaHarvesters());
        containerWidget.setWidget(getView().asWidget());
        fetchHarvesters();
    }

    /**
     * This method fetches all harvesters and sends them to the view
     */
    private void fetchHarvesters() {
        commonInjector.getFlowStoreProxyAsync()
                .findAllInfomediaHarvesterConfigs(new FetchHarvesterConfigsCallback());
    }

    private View getView() {
        return viewInjector.getView();
    }

    @Override
    public void createInfomediaHarvester() {
        placeController.goTo(new CreatePlace());
    }

    @Override
    public void editInfomediaHarvesterConfig(String id) {
        placeController.goTo(new EditPlace(id));
    }

    class FetchHarvesterConfigsCallback extends FilteredAsyncCallback<List<InfomediaHarvesterConfig>> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    caught, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<InfomediaHarvesterConfig> infomediaHarvesterConfigs) {
            getView().setHarvesters(infomediaHarvesterConfigs);
        }
    }

}
