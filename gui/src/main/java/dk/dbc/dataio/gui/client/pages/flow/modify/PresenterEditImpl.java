package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowModel;

/**
 * Concrete Presenter Implementation Class for Flow Binder Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {

    private long id;

    /**
     * Constructor
     *
     * @param place           the place
     * @param placeController PlaceController for navigation
     * @param header          Breadcrumb header text
     */
    public PresenterEditImpl(Place place, PlaceController placeController, String header) {
        super(placeController, header);
        id = place.getFlowId();

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
     * The method fetches the stored Flow Binder, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getFlow(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a Flow Binder in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateFlow(getView().model, new SaveFlowModelAsyncCallback());
    }

    /**
     * Deletes the embedded model as a Flow in the database
     */
    void deleteModel() {
        commonInjector.getFlowStoreProxyAsync().deleteFlow(getView().model.getId(), getView().model.getVersion(), new DeleteFlowModelFilteredAsyncCallback());
    }

    // Private methods
    private void getFlow(final long flowId) {
        commonInjector.getFlowStoreProxyAsync().getFlow(flowId, new GetFlowModelAsyncCallback());
    }

    /**
     * A signal to the presenter, saying that the delete button has been pressed
     */
    public void deleteButtonPressed() {
        deleteModel();
    }

    /**
     * Call back class to be instantiated in the call to getFlow in flowstore proxy
     */
    class GetFlowModelAsyncCallback extends FilteredAsyncCallback<FlowModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Flow.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(FlowModel model) {
            if (!getView().showAvailableFlowComponents) {
                setFlowModel(model);
            }
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to deleteFlow in flowstore proxy
     */
    class DeleteFlowModelFilteredAsyncCallback extends FilteredAsyncCallback<Void> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_FlowSuccessfullyDeleted());
            setFlowModel(new FlowModel());
            History.back();
        }
    }
}
