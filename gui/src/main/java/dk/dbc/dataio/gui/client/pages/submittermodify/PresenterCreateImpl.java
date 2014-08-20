package dk.dbc.dataio.gui.client.pages.submittermodify;

import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Submitter Create
 */
public class PresenterCreateImpl extends PresenterImpl {
    private final static Long DEFAULT_ID = 0L;
    private final static Long DEFAULT_VERSION = 1L;
    private final static String DEFAULT_NUMBER = "";
    private final static String DEFAULT_NAME = "";
    private final static String DEFAULT_DESCRIPTION = "";

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
    public void getModel() {
        model = new Model(DEFAULT_ID, DEFAULT_VERSION, DEFAULT_NUMBER, DEFAULT_NAME, DEFAULT_DESCRIPTION);
    }

    /**
     * saveModel
     * Saves the embedded model as a new Submitter in the database
     */
    @Override
    void saveModel() {
        try {
            model.validate(constants);
            final SubmitterContent submitterContent = new SubmitterContent(Long.parseLong(model.getNumber()), model.getName(), model.getDescription());
            flowStoreProxy.createSubmitter(submitterContent, new FilteredAsyncCallback<Submitter>() {
                @Override
                public void onFilteredFailure(Throwable e) {
                    view.setErrorText(getErrorText(e));
                }

                @Override
                public void onSuccess(Submitter submitter) {
                    view.setStatusText(constants.status_SubmitterSuccessfullySaved());
                }
            });

        } catch (IllegalArgumentException e) {
            view.setErrorText(e.getMessage());
        }
    }

}
