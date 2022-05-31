package dk.dbc.dataio.gui.client.pages.harvester.infomedia.modify;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;

public class PresenterCreateImpl<Place extends EditPlace> extends PresenterImpl {

    public PresenterCreateImpl(String header) {
        super(header);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
    }

    @Override
    public void initializeModel() {
        final InfomediaHarvesterConfig config = new InfomediaHarvesterConfig(
                1,
                1,
                new InfomediaHarvesterConfig.Content()
                        .withId("")
                        .withSchedule("")
                        .withDescription("")
                        .withDestination("")
                        .withFormat("")
                        .withEnabled(false));
        setInfomediaHarvesterConfig(config);
        updateAllFieldsAccordingToCurrentState();
    }

    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createInfomediaHarvesterConfig(
                config, new CreateHarvesterConfigAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {
    }

    class CreateHarvesterConfigAsyncCallback implements AsyncCallback<InfomediaHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "HarvesterConfig.id: [new Harvester]";
            getView().setErrorText(ProxyErrorTranslator
                    .toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(InfomediaHarvesterConfig harvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }
}
