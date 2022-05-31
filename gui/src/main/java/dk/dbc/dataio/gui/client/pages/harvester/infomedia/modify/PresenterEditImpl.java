package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;


public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;

    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getHarvesterId();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
        getView().deleteButton.setVisible(true);
    }

    @Override
    public void initializeModel() {
        commonInjector.getFlowStoreProxyAsync()
                .getInfomediaHarvesterConfig(id, new GetInfomediaHarvesterConfigAsyncCallback());
    }

    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateHarvesterConfig(config,
                new UpdateInfomediaHarvesterConfigAsyncCallback());
    }

    @Override
    public void deleteButtonPressed() {
        commonInjector.getFlowStoreProxyAsync().deleteHarvesterConfig(config.getId(), config.getVersion(),
                new DeleteInfomediaHarvesterConfigAsyncCallback());
    }

    class GetInfomediaHarvesterConfigAsyncCallback implements AsyncCallback<InfomediaHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "InfomediaHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(InfomediaHarvesterConfig config) {
            if (config == null) {
                getView().setErrorText(getTexts().error_HarvesterNotFound());
            } else {
                setInfomediaHarvesterConfig(config);
                updateAllFieldsAccordingToCurrentState();
            }
        }
    }

    class UpdateInfomediaHarvesterConfigAsyncCallback implements AsyncCallback<HarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "InfomediaHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(HarvesterConfig config) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }

    class DeleteInfomediaHarvesterConfigAsyncCallback implements AsyncCallback<Void> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "InfomediaHarvesterConfig.id: " + config.getId();
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromTickleHarvesterProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_ConfigSuccessfullyDeleted());
            setInfomediaHarvesterConfig(null);
            History.back();
        }
    }
}
