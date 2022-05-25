package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.components.prompted.PromptedMultiList;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SinkModel;

import java.util.Collection;

/**
 * Concrete Presenter Implementation Class for Sink Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;

    /**
     * Constructor
     *
     * @param place  the edit place
     * @param header header
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getSinkId();
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
     * The method fetches the stored Sink, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getSink(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a Sink in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateSink(model, new SaveSinkModelFilteredAsyncCallback());
    }

    /**
     * Deletes the embedded model as a Sink in the database
     */
    void deleteModel() {
        commonInjector.getFlowStoreProxyAsync().deleteSink(model.getId(), model.getVersion(), new DeleteSinkModelFilteredAsyncCallback());
    }

    // Private methods
    private void getSink(final long sinkId) {
        commonInjector.getFlowStoreProxyAsync().getSink(sinkId, new GetSinkModelFilteredAsyncCallback());
    }

    /**
     * A signal to the presenter, saying that the delete button has been pressed
     */
    public void deleteButtonPressed() {
        if (model != null) {
            deleteModel();
        }
    }

    /**
     * Call back class to be instantiated in the call to getSink in flowstore proxy
     */
    class GetSinkModelFilteredAsyncCallback extends FilteredAsyncCallback<SinkModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Sink.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(SinkModel model) {
            setSinkModel(model);
            updateAllFieldsAccordingToCurrentState();
            handleSinkConfig(model.getSinkType());
        }
    }

    @Override
    void handleSinkConfig(SinkContent.SinkType sinkType) {
        View view = getView();
        view.sinkTypeSelection.setEnabled(false);
        switch (sinkType) {
            case DPF:
                view.dpfUpdateServiceUserId.setText(model.getDpfUpdateServiceUserId());
                view.dpfUpdateServicePassword.setText(model.getDpfUpdateServicePassword());
                populateMultiList(view.dpfUpdateServiceQueueProviders, model.getDpfUpdateServiceAvailableQueueProviders());
                view.dpfSinkSection.setVisible(true);
                view.sequenceAnalysisSection.setVisible(true);
                break;
            case OPENUPDATE:
                view.url.setText(model.getOpenUpdateEndpoint());
                view.openupdateuserid.setText(model.getOpenUpdateUserId());
                view.openupdatepassword.setText(model.getOpenUpdatePassword());
                populateMultiList(view.queueProviders, model.getOpenUpdateAvailableQueueProviders());
                populateMultiList(view.updateServiceIgnoredValidationErrors, model.getUpdateServiceIgnoredValidationErrors());
                view.updateSinkSection.setVisible(true);
                view.sequenceAnalysisSection.setVisible(true);
                break;
            case ES:
                view.esUserId.setText(String.valueOf(model.getEsUserId()));
                view.esDatabase.setText(model.getEsDatabase());
                view.esSinkSection.setVisible(true);
                view.sequenceAnalysisSection.setVisible(true);
                break;
            case IMS:
                view.imsEndpoint.setText(model.getImsEndpoint());
                view.imsSinkSection.setVisible(true);
                view.sequenceAnalysisSection.setVisible(true);
                break;
            case WORLDCAT:
                view.worldCatUserId.setText(model.getWorldCatUserId());
                view.worldCatPassword.setText(model.getWorldCatPassword());
                view.worldCatProjectId.setText(model.getWorldCatProjectId());
                view.worldCatEndpoint.setText(model.getWorldCatEndpoint());
                populateMultiList(view.worldCatRetryDiagnostics, model.getWorldCatRetryDiagnostics());
                view.sequenceAnalysisSection.setVisible(true);
                view.worldCatSinkSection.setVisible(true);
            case TICKLE:
                view.sequenceAnalysisSection.setVisible(false);
                break;
            case DUMMY:
                view.sequenceAnalysisSection.setVisible(true);
                break;
            case VIP:
                view.vipEndpoint.setText(model.getVipEndpoint());
                view.vipSinkSection.setVisible(true);
                view.sequenceAnalysisSection.setVisible(false);
                break;
            case DMAT:
                view.sequenceAnalysisSection.setVisible(false);
                break;
        }
    }

    private void populateMultiList(PromptedMultiList multiList, Collection<String> values) {
        multiList.clear();
        if (values != null) {
            for (String value : values) {
                multiList.addValue(value, value);
            }
        }
    }

    /**
     * Local call back class to be instantiated in the call to deleteSink in flowstore proxy
     */
    class DeleteSinkModelFilteredAsyncCallback extends FilteredAsyncCallback<Void> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_SinkSuccessfullyDeleted());
            setSinkModel(null);
            History.back();
        }
    }
}
