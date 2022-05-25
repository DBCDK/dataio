package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;


/**
 * Concrete Presenter Implementation Class for Harvester Edit
 */
public class PresenterCreateImpl<Place extends EditPlace> extends PresenterImpl {
    /**
     * Constructor
     *
     * @param header The header
     */
    public PresenterCreateImpl(String header) {
        super(header);

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
    }

    /**
     * Initializing the model
     * The method fetches the stored Harvester Configuration, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {

        RRHarvesterConfig rrHarvesterConfig = new RRHarvesterConfig(1, 1, new RRHarvesterConfig.Content()
                .withEnabled(false)
                .withType(JobSpecification.Type.TRANSIENT)
                .withIncludeRelations(true)
                .withIncludeLibraryRules(false)
                .withBatchSize(10000)
        );
        setRRHarvesterConfig(rrHarvesterConfig);
        updateAllFieldsAccordingToCurrentState();

    }

    /**
     * saveModel
     * Updates the embedded model as a Harvester Config in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createRRHarvesterConfig(model, new UpdateHarvesterConfigAsyncCallback());
    }


    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {
    }


    /*
     * Private classes
     */

    class UpdateHarvesterConfigAsyncCallback implements AsyncCallback<RRHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "HarvesterConfig.id: [new Harvester]";
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(RRHarvesterConfig harvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }

}
