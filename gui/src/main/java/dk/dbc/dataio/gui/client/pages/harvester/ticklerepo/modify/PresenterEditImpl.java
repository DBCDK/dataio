package dk.dbc.dataio.gui.client.pages.harvester.ticklerepo.modify;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.log.LogPanel;
import dk.dbc.dataio.gui.client.components.log.LogPanelMessages;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.pages.job.show.ShowAcctestJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.pages.job.show.ShowTestJobsPlace;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

import static dk.dbc.dataio.gui.client.views.ContentPanel.GUIID_CONTENT_PANEL;


/**
 * Concrete Presenter Implementation Class for Harvester Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;
    private PlaceController placeController;

    /**
     * Constructor
     *
     * @param placeController the placeController
     * @param place           the edit place
     * @param header          the header
     */
    public PresenterEditImpl(PlaceController placeController, Place place, String header) {
        super(header);
        id = place.getHarvesterId();
        this.placeController = placeController;
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
        getView().taskRecordHarvestButton.setVisible(true);
        getView().deleteOutdatedRecordsButton.setVisible(false);
    }

    /**
     * Initializing the model
     * The method fetches the stored Harvester Configuration, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        commonInjector.getFlowStoreProxyAsync().getTickleRepoHarvesterConfig(id, new GetTickleRepoHarvesterConfigAsyncCallback());
    }

    /**
     * saveModel
     * Updates the embedded model as a Harvester Config in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateHarvesterConfig(config, new UpdateTickleRepoHarvesterConfigAsyncCallback());
    }

    /**
     * deleteButtonPressed
     * Deletes the current Harvester Config in the database
     */
    @Override
    public void deleteButtonPressed() {
        commonInjector.getFlowStoreProxyAsync().deleteHarvesterConfig(config.getId(), config.getVersion(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable e) {
                String msg = "TickleRepoHarvesterConfig.id: " + config.getId();
                getView().setErrorText(ProxyErrorTranslator.toClientErrorFromTickleHarvesterProxy(e, commonInjector.getProxyErrorTexts(), msg));
            }

            @Override
            public void onSuccess(Void aVoid) {
                getView().status.setText(getTexts().status_TickleRepoHarvesterSuccessfullyDeleted());
                setTickleRepoHarvesterConfig(null);
                History.back();
            }
        });
    }

    /**
     * Creates task record harvest
     */
    @Override
    public void taskRecordHarvestButtonPressed() {
        commonInjector.getTickleHarvesterProxyAsync().createHarvestTask(config, new CreateHarvestTaskAsyncCallback());
    }

    @Override
    public void deleteOutdatedRecordsButtonPressed() {
        getView().deleteOutdatedRecordsDialog.setVisible(true);
        getView().deleteOutdatedRecordsDialog.show();
    }

    @Override
    public void deleteOutdatedRecords() {
        final String fromDate = getView().deleteOutdatedRecordsFromDate.getValue();
        if (fromDate == null || fromDate.isEmpty()) {
            getView().setErrorText(getTexts().error_DeleteOutdatedRecordsFromDateValidationError());
        } else {
            getView().status.setText(getTexts().status_Busy());
            commonInjector.getTickleHarvesterProxyAsync().deleteOutdatedRecords(
                    config.getContent().getDatasetName(), Format.parseLongDateAsLong(fromDate),
                    new DeleteOutdatedRecordsAsyncCallback());
        }
    }

    /**
     * Sets task record count
     */
    @Override
    public void setRecordHarvestCount() {
        getView().status.setText(getTexts().status_Busy());
        commonInjector.getTickleHarvesterProxyAsync().getDataSetSizeEstimate(config.getContent().getDatasetName(), new GetDataSetSizeEstimateAsyncCallback());
    }

    private void showDeleteOutdatedRecordsButtonForApplicableDataset() {
        final String[] canDeleteOutdatedRecords = {"masterfile"};
        for (String matchString : canDeleteOutdatedRecords) {
            if (config.getContent().getDatasetName().toLowerCase().contains(matchString)) {
                getView().deleteOutdatedRecordsButton.setVisible(true);
                break;
            }
        }
    }

    class GetTickleRepoHarvesterConfigAsyncCallback implements AsyncCallback<TickleRepoHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "TickleRepoHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(TickleRepoHarvesterConfig tickleRepoHarvesterConfig) {
            if (tickleRepoHarvesterConfig == null) {
                getView().setErrorText(getTexts().error_HarvesterNotFound());
            } else {
                setTickleRepoHarvesterConfig(tickleRepoHarvesterConfig);
                updateAllFieldsAccordingToCurrentState();
                showDeleteOutdatedRecordsButtonForApplicableDataset();
            }
        }
    }

    class UpdateTickleRepoHarvesterConfigAsyncCallback implements AsyncCallback<HarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "TickleRepoHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(HarvesterConfig harvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }

    class CreateHarvestTaskAsyncCallback implements AsyncCallback<Void> {
        @Override
        public void onFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), e.getMessage() + e.getStackTrace()));
            setLogMessage(e.getMessage());
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_HarvestTaskCreated());
            goToTypeOfJobPlace(config.getContent().getType());
            setLogMessage(LogPanelMessages.harvestTaskCreated(config.getContent().getDatasetName(), config.getContent().getDestination()));
        }
    }

    class GetDataSetSizeEstimateAsyncCallback implements AsyncCallback<Integer> {
        @Override
        public void onFailure(Throwable e) {
            getView().status.setText("");
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), e.getMessage() + e.getStackTrace()));
            setLogMessage(e.getMessage());
        }

        @Override
        public void onSuccess(Integer integer) {
            getView().status.setText("");
            final Texts texts = getTexts();
            final String text = texts.dialog_numberOfRecords().replace("$1", String.valueOf(integer)).replace("$2", integer == 1 ? texts.label_Record() : texts.label_Records());
            getView().recordHarvestCount.setText(text);
            getView().recordHarvestConfirmationDialog.setVisible(true);
            getView().recordHarvestConfirmationDialog.show();
        }
    }

    class DeleteOutdatedRecordsAsyncCallback implements AsyncCallback<Void> {
        @Override
        public void onFailure(Throwable e) {
            getView().status.setText("");
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(),
                    e.getMessage() + e.getStackTrace()));
            setLogMessage(e.getMessage());
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_DeleteOutdatedRecords());
        }
    }

    private void goToTypeOfJobPlace(JobSpecification.Type type) {
        switch (type) {
            case ACCTEST:
                placeController.goTo(new ShowAcctestJobsPlace()); // ACCTEST
                break;
            case TEST:
                placeController.goTo(new ShowTestJobsPlace());       // TEST
                break;
            default:
                placeController.goTo(new ShowJobsPlace());             // PERSISTENT and TRANSIENT
        }
    }

    private void setLogMessage(String message) {
        LogPanel logPanel = ((ContentPanel) Document.get().getElementById(GUIID_CONTENT_PANEL).getPropertyObject(GUIID_CONTENT_PANEL)).getLogPanel();
        logPanel.clear();
        logPanel.showMessage(message);
    }

}
