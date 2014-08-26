package dk.dbc.dataio.gui.client.pages.submittermodify;

import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Submitter Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     * @param clientFactory, clientFactory
     * @param constants, the constants for submitter modify
     */
    public PresenterCreateImpl(ClientFactory clientFactory, SubmitterModifyConstants constants) {
        super(clientFactory, constants);
        view = clientFactory.getSubmitterCreateView();
    }

    /**
     * getModel - initializes the model
     * When starting the form, the fields shall be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new SubmitterModel();
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Submitter in the database
     */
    @Override
    void saveModel() {
        if (!model.isNumberValid()) {
            view.setErrorText(constants.error_NumberInputFieldValidationError());
        } else {
            flowStoreProxy.createSubmitter(model, new SaveSubmitterModelFilteredAsyncCallback());
        }
    }

}
