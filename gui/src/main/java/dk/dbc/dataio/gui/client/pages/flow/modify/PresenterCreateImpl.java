package dk.dbc.dataio.gui.client.pages.flow.modify;


import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * Concrete Presenter Implementation Class for Flow Create
 */
public class PresenterCreateImpl extends PresenterImpl {

    /**
     * Constructor
     * @param clientFactory, clientFactory
     */
    public PresenterCreateImpl(ClientFactory clientFactory) {
        super(clientFactory);
        view = clientFactory.getFlowCreateView();
    }

    /**
     * initializeModel - initializes the model
     * When starting the form, the fields shall be empty, therefore an empty Model is instantiated
     */
    @Override
    public void initializeModel() {
        model = new FlowModel();
        updateAllFieldsAccordingToCurrentState();
    }

    /**
     * saveModel
     * Saves the embedded model as a new Flow in the database
     */
    @Override
    void saveModel() {
        flowStoreProxy.createFlow(model, new SaveFlowModelAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {}
}
