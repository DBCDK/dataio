package dk.dbc.dataio.gui.client.pages.flowbinder.modify;


import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flowbinder Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     * @param clientFactory, clientFactory
     * @param constants, the constants for submitter modify
     */
    public PresenterCreateImpl(ClientFactory clientFactory, Texts constants) {
        super(clientFactory, constants);
        view = clientFactory.getFlowbinderCreateView();
    }

    /**
     * initializeModel - initializes the model
     * When starting the form, the fields shall be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new FlowBinderModel();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Flowbinder in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.createFlowBinder(model, new SaveFlowBinderModelFilteredAsyncCallback());
    }


}
