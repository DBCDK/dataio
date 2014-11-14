package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.place.shared.Place;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Submitter Edit
 */
public class PresenterEditImpl extends PresenterImpl {
    private Long id;

    /**
     * Constructor
     * @param clientFactory
     * @param texts
     */
    public PresenterEditImpl(Place place, ClientFactory clientFactory, Texts texts) {
        super(clientFactory, texts);
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
        if (!model.isNumberValid()) {
            view.setErrorText(texts.error_NumberInputFieldValidationError());
        } else {
            flowStoreProxy.updateSubmitter(model, new SaveSubmitterModelFilteredAsyncCallback());
        }
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
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CannotFetchSubmitter());
        }

        @Override
        public void onSuccess(SubmitterModel model) {
            setSubmitterModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }

}
