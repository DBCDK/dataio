package dk.dbc.dataio.gui.client.pages.submittermodify;

import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
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
    }

    /**
     * saveModel
     * Saves the embedded model as a new Submitter in the database
     */
    @Override
    void saveModel() {
        if(!model.isNumberValid()) {
            view.setErrorText("Could not translate (String)model.number to long value.");
        } else {
//            try {
                flowStoreProxy.createSubmitter(model, new FilteredAsyncCallback<SubmitterModel>() {
                    @Override
                    public void onFilteredFailure(Throwable e) {
                        view.setErrorText(getErrorText(e));
                    }

                    @Override
                    public void onSuccess(SubmitterModel model) {
                        view.setStatusText(constants.status_SubmitterSuccessfullySaved());
                    }
                });
//            } catch (ProxyException e) {
//                view.setErrorText(e.getMessage());
//            }
        }

    }

}
