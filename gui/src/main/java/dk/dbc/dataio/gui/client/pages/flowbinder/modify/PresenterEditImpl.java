package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Binder Edit
 */
public class PresenterEditImpl extends PresenterImpl {

    private long id;
    /**
     * Constructor
     * @param clientFactory the clientFactory
     * @param texts The text String used by flow
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory, Texts texts) {
        super(clientFactory, texts);
        view = clientFactory.getFlowBinderEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getFlowBinderId();
    }

    /**
     * Initializing the model
     * The method fetches the stored Flow Binder, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getFlowBinder(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a Flow Binder in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.updateFlowBinder(model, new SaveFlowBinderModelFilteredAsyncCallback());
    }

    // Private methods
    private void getFlowBinder(final long flowBinderId) {
        flowStoreProxy.getFlowBinder(flowBinderId, new GetFlowBinderModelFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getFlowBinder in flowstore proxy
     */
    class GetFlowBinderModelFilteredAsyncCallback extends FilteredAsyncCallback<FlowBinderModel> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CannotFetchFlowBinder());
        }
        @Override
        public void onSuccess(FlowBinderModel model) {
            setFlowBinderModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }
}
