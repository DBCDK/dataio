package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;


/**
 * Concrete Presenter Implementation Class for Harvester Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;

    /**
     * Constructor
     *
     * @param place  the edit place
     * @param header The header
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getHarvesterId();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
        getView().deleteButton.setVisible(true);
    }

    /**
     * Initializing the model
     * The method fetches the stored Harvester Configuration, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        commonInjector.getFlowStoreProxyAsync().getRRHarvesterConfig(id, new GetRRHarvesterConfigAsyncCallback());
    }

    /**
     * saveModel
     * Updates the embedded model as a Harvester Config in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateHarvesterConfig(model, new UpdateHarvesterConfigAsyncCallback());
    }

    /**
     * deleteButtonPressed
     * Deletes the current Harvester Config in the database
     */
    @Override
    public void deleteButtonPressed() {
        commonInjector.getFlowStoreProxyAsync().deleteHarvesterConfig(model.getId(), model.getVersion(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                String msg = "RRHarvesterConfig.id: " + model.getId();
                getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
            }

            @Override
            public void onSuccess(Void aVoid) {
                getView().status.setText(getTexts().status_RRHarvesterSuccessfullyDeleted());
                setRRHarvesterConfig(null);
                History.back();
            }
        });
    }


    /*
     * Private classes
     */

    class GetRRHarvesterConfigAsyncCallback implements AsyncCallback<RRHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "RRHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(RRHarvesterConfig rrHarvesterConfig) {
            if (rrHarvesterConfig == null) {
                getView().setErrorText(getTexts().error_HarvesterNotFound());
            } else {
                setRRHarvesterConfig(rrHarvesterConfig);
                updateAllFieldsAccordingToCurrentState();
            }
        }
    }

    class UpdateHarvesterConfigAsyncCallback implements AsyncCallback<HarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "HarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(HarvesterConfig harvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }

}
