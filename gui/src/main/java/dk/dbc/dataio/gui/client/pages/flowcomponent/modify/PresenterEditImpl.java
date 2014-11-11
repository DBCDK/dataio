package dk.dbc.dataio.gui.client.pages.flowcomponent.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Component Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private long id;

    /**
     * Constructor
     * @param clientFactory the clientFactory
     * @param texts The text String used by flow component
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory, Texts texts) {
        super(clientFactory, texts);
        view = clientFactory.getFlowComponentEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getFlowComponentId();
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
        flowStoreProxy.updateFlowComponent(model, new SaveFlowComponentModelFilteredAsyncCallback());
    }

    // Private methods
    private void getFlowComponent(final long flowComponentId) {
        flowStoreProxy.getFlowComponent(flowComponentId, new GetFlowComponentModelFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getFlowComponent in flowStoreProxy
     */
    class GetFlowComponentModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowComponentModel> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CannotFetchFlowComponent());
        }
        @Override
        public void onSuccess(FlowComponentModel model) {
            setFlowComponentModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }
}
