package dk.dbc.dataio.gui.client.pages.harvester.dmat.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterConfig;


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
                .getDMatHarvesterConfig(id, new GetDMatHarvesterConfigAsyncCallback());
    }

    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync()
                .updateHarvesterConfig(config, new UpdateDMatHarvesterConfigAsyncCallback());
    }

    class GetDMatHarvesterConfigAsyncCallback implements AsyncCallback<DMatHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "DMatHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(DMatHarvesterConfig config) {
            if (config == null) {
                getView().setErrorText(getTexts().error_HarvesterNotFound());
            } else {
                setConfig(config);
            }
        }
    }

    class UpdateDMatHarvesterConfigAsyncCallback implements AsyncCallback<HarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "DMatHarvesterConfig.id: " + id;
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
