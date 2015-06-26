package dk.dbc.dataio.gui.client.pages.flow.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Binder Edit
 */
public class PresenterEditImpl extends PresenterImpl {

    private long id;
    /**
     * Constructor
     * @param place the place
     * @param clientFactory the clientFactory
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getFlowEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getFlowId();
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
        flowStoreProxy.updateFlow(model, new SaveFlowModelAsyncCallback());
    }

    // Private methods
    private void getFlow(final long flowId) {
        flowStoreProxy.getFlow(flowId, new GetFlowModelAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getFlow in flowstore proxy
     */
    class GetFlowModelAsyncCallback extends FilteredAsyncCallback<FlowModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Flow.id: " + id;
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, msg));
        }
        @Override
        public void onSuccess(FlowModel model) {
            setFlowModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }
}
