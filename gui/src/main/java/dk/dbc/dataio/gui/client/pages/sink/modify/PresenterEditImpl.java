package dk.dbc.dataio.gui.client.pages.sink.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Sink Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private long id;

    /**
     * Constructor
     * @param clientFactory
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getSinkEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getSinkId();
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
        flowStoreProxy.updateSink(model, new SaveSinkModelFilteredAsyncCallback());
    }

    // Private methods
    private void getSink(final long sinkId) {
        flowStoreProxy.getSink(sinkId, new GetSinkModelFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getSink in flowstore proxy
     */
    class GetSinkModelFilteredAsyncCallback extends FilteredAsyncCallback<SinkModel> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CannotFetchSink());
        }

        @Override
        public void onSuccess(SinkModel model) {
            setSinkModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }
}
