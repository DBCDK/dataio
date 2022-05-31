package dk.dbc.dataio.gui.client.pages.submitter.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

/**
 * Concrete Presenter Implementation Class for Submitter Edit
 */
public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private Long id;

    /**
     * Constructor
     *
     * @param place  the place
     * @param header Breadcrumb header text
     */
    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getSubmitterId();
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
        commonInjector.getFlowStoreProxyAsync().updateSubmitter(model, new SaveSubmitterModelFilteredAsyncCallback());
    }

    void deleteModel() {
        commonInjector.getFlowStoreProxyAsync().deleteSubmitter(model.getId(), model.getVersion(), new DeleteSubmitterModelFilteredAsyncCallback());
    }

    // Private methods
    private void getSubmitter(final Long submitterId) {
        commonInjector.getFlowStoreProxyAsync().getSubmitter(submitterId, new GetSubmitterModelFilteredAsyncCallback());
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void deleteButtonPressed() {
        if (model != null) {
            if (!model.isNumberValid()) {
                getView().setErrorText(getTexts().error_NumberInputFieldValidationError());
            } else {
                deleteModel();
            }
        }
    }

    /**
     * Call back class to be instantiated in the call to getSubmitter in flowstore proxy
     */
    class GetSubmitterModelFilteredAsyncCallback extends FilteredAsyncCallback<SubmitterModel> {
        @Override
        public void onFilteredFailure(Throwable e) {
            String msg = "Submitter.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }

        @Override
        public void onSuccess(SubmitterModel model) {
            setSubmitterModel(model);
            updateAllFieldsAccordingToCurrentState();
        }
    }

    /**
     * Local call back class to be instantiated in the call to createSubmitter or updateSubmitter in flowstore proxy
     */
    class DeleteSubmitterModelFilteredAsyncCallback extends FilteredAsyncCallback<Void> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(Void aVoid) {
            getView().status.setText(getTexts().status_SubmitterSuccessfullyDeleted());
            setSubmitterModel(null);
            History.back();
        }
    }

}
