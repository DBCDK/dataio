package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;

/**
 * Concrete Presenter Implementation Class for Flow Component Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;

    /**
     * Constructor
     *
     * @param place  the place
     * @param header Breadcrumb header text
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getFlowComponentId();
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
     * The method fetches the stored flow component, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getFlowComponent(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a flow component in the database
     */
    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().updateFlowComponent(model, new SaveFlowComponentModelFilteredAsyncCallback());
    }

    /**
     * A signal to the presenter, saying that the delete button has been pressed
     */
    public void deleteButtonPressed() {
        if (model != null) {
            deleteModel();
        }
    }

    // Private methods
    private void getFlowComponent(final long flowComponentId) {
        commonInjector.getFlowStoreProxyAsync().getFlowComponent(flowComponentId, new GetFlowComponentModelFilteredAsyncCallback());
    }

    /**
     * Deletes the embedded model as a FlowBinder in the database
     */
    void deleteModel() {
        commonInjector.getFlowStoreProxyAsync().deleteFlowComponent(model.getId(), model.getVersion(), new DeleteFlowComponentFilteredAsyncCallback());
    }


    /**
     * Call back class to be instantiated in the call to getFlowComponent in flowStoreProxy
     */
    class GetFlowComponentModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowComponentModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Flowcomponent.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(FlowComponentModel model) {
            setFlowComponentModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to deleteFlowBinder in flowstore proxy
     */
    class DeleteFlowComponentFilteredAsyncCallback extends FilteredAsyncCallback<Void> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_FlowComponentSuccessfullyDeleted());
            setFlowComponentModel(null);
            History.back();
        }
    }
}
