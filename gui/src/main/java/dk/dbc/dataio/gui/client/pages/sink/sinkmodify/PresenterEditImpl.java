package dk.dbc.dataio.gui.client.pages.sink.sinkmodify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Sink Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private long id;

    /**
     * Constructor
     * @param clientFactory
     * @param constants
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory, SinkModifyConstants constants) {
        super(clientFactory, constants);
        view = clientFactory.getSinkEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getSinkId();
    }
    /**
     * Initializing the model
     * The method fetches the stored Submitter, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getSink(id);
    }

    /**
     * saveModel
     * Updates the embedded model as a Submitter in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.updateSink(model, new SaveSinkModelFilteredAsyncCallback());
    }

    // Private methods
    private void getSink(final long sinkId) {
        flowStoreProxy.getSink(sinkId, new GetSinkModelFilteredAsyncCallback());
    }

    private void setSinkModel(SinkModel model) {
        this.model = model;
    }


    /**
     * Call back class to be instantiated in the call to getSubmitter in flowstore proxy
     */
    class GetSinkModelFilteredAsyncCallback extends FilteredAsyncCallback<SinkModel> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(constants.error_CannotFetchSink());
        }

        @Override
        public void onSuccess(SinkModel model) {
            setSinkModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }
}
