package dk.dbc.dataio.gui.client.pages.harvester.promat.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;


public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;

    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getHarvesterId();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
    }

    @Override
    public void initializeModel() {
        commonInjector.getFlowStoreProxyAsync()
                .getPromatHarvesterConfig(id, new GetPromatHarvesterConfigAsyncCallback());
    }

    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync()
                .updateHarvesterConfig(config, new UpdatePromatHarvesterConfigAsyncCallback());
    }

    class GetPromatHarvesterConfigAsyncCallback implements AsyncCallback<PromatHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "PromatHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(PromatHarvesterConfig config) {
            if (config == null) {
                getView().setErrorText(getTexts().error_HarvesterNotFound());
            } else {
                setConfig(config);
            }
        }
    }

    class UpdatePromatHarvesterConfigAsyncCallback implements AsyncCallback<HarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "PromatHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(HarvesterConfig config) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }
}
