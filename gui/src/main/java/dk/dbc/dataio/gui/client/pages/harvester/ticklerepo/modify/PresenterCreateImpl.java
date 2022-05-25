package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

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

        TickleRepoHarvesterConfig tickleRepoHarvesterConfig = new TickleRepoHarvesterConfig(
                1,
                1,
                new TickleRepoHarvesterConfig.Content()
                        .withId("")
                        .withDatasetName("")
                        .withDescription("")
                        .withDestination("")
                        .withFormat("")
                        .withType(JobSpecification.Type.TRANSIENT)
                        .withEnabled(false)
        );
        setTickleRepoHarvesterConfig(tickleRepoHarvesterConfig);
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Updates the embedded model as a Harvester Config in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createTickleRepoHarvesterConfig(config, new CreateHarvesterConfigAsyncCallback());
    }


    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {
    }

    /**
     * This has no implementation because "Create" does not have a task record harvest button!
     */
    public void taskRecordHarvestButtonPressed() {
    }

    /**
     * This has no implementation because "Create" does not have a delete outdated records button!
     */
    @Override
    public void deleteOutdatedRecordsButtonPressed() {
    }

    /**
     * This has no implementation because "Create" does not have a delete outdated records button!
     */
    @Override
    public void deleteOutdatedRecords() {
    }

    /**
     * This has no implementation because "Create" does not have a task record harvest button!
     */
    public void setRecordHarvestCount() {
    }


    /*
     * Private classes
     */

    class CreateHarvesterConfigAsyncCallback implements AsyncCallback<TickleRepoHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "HarvesterConfig.id: [new Harvester]";
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(TickleRepoHarvesterConfig harvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }

}
