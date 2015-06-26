package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Submitter Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private Long id;

    /**
     * Constructor
     * @param place, the place
     * @param clientFactory, the client factory
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getSubmitterEditView();
        EditPlace editPlace = (EditPlace) place;
        id = editPlace.getSubmitterId();
    }


    /**
     * Initializing the model
     * The method fetches the stored Submitter, as given in the Place (referenced by this.id)
     */
    @Override
    public void initializeModel() {
        getSubmitter(id);
    }


    /**
     * saveModel
     * Updates the embedded model as a Submitter in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.updateSubmitter(model, new SaveSubmitterModelFilteredAsyncCallback());
    }

    // Private methods
    private void getSubmitter(final Long submitterId) {
        flowStoreProxy.getSubmitter(submitterId, new GetSubmitterModelFilteredAsyncCallback());
    }

    /**
     * Call back class to be instantiated in the call to getSubmitter in flowstore proxy
     */
    class GetSubmitterModelFilteredAsyncCallback extends FilteredAsyncCallback<SubmitterModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Submitter.id: " + id;
            view.setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, proxyErrorTexts, msg));
        }

        @Override
        public void onSuccess(SubmitterModel model) {
            setSubmitterModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }

}
